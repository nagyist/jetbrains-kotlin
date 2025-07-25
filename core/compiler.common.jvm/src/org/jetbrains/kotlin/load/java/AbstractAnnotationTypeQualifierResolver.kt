/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.ConcurrentHashMap

private typealias TypeQualifierWithApplicability<TAnnotation> = Pair<TAnnotation, Set<AnnotationQualifierApplicabilityType>>

abstract class AbstractAnnotationTypeQualifierResolver<TAnnotation : Any>(
    private val javaTypeEnhancementState: JavaTypeEnhancementState
) {
    protected abstract val TAnnotation.metaAnnotations: Iterable<TAnnotation>
    protected abstract val TAnnotation.key: Any // TODO: figure out if keying `resolvedNicknames` by `fqName` is safe
    protected abstract val TAnnotation.fqName: FqName?
    protected abstract fun TAnnotation.enumArguments(onlyValue: Boolean): Iterable<String>

    private fun TAnnotation.findAnnotation(fqName: FqName): TAnnotation? =
        metaAnnotations.find { it.fqName == fqName }

    private fun TAnnotation.hasAnnotation(fqName: FqName): Boolean =
        metaAnnotations.any { it.fqName == fqName }

    private val resolvedNicknames = ConcurrentHashMap<Any, TAnnotation>()

    abstract val isK2: Boolean

    fun resolveTypeQualifierAnnotation(annotation: TAnnotation): TAnnotation? {
        if (javaTypeEnhancementState.jsr305.isDisabled) return null
        if (annotation.fqName in BUILT_IN_TYPE_QUALIFIER_ANNOTATIONS || annotation.hasAnnotation(JAVAX_TYPE_QUALIFIER_ANNOTATION_FQ_NAME))
            return annotation
        if (!annotation.hasAnnotation(JAVAX_TYPE_QUALIFIER_NICKNAME_ANNOTATION_FQ_NAME))
            return null
        return resolvedNicknames.getOrPut(annotation.key) {
            // This won't store nulls (ConcurrentHashMap does not permit that), but presumably unless the code
            // is broken a nickname should be resolvable.
            annotation.metaAnnotations.firstNotNullOfOrNull(::resolveTypeQualifierAnnotation) ?: return null
        }
    }

    private fun resolveQualifierBuiltInDefaultAnnotation(annotation: TAnnotation, isForWarningOnly: Boolean): JavaDefaultQualifiers? {
        if (javaTypeEnhancementState.disabledDefaultAnnotations) {
            return null
        }

        return BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATIONS[annotation.fqName]?.let { qualifierForDefaultingAnnotation ->
            val state = resolveDefaultAnnotationState(annotation).takeIf { it != ReportLevel.IGNORE } ?: return null
            qualifierForDefaultingAnnotation.copy(
                nullabilityQualifier = qualifierForDefaultingAnnotation.nullabilityQualifier
                    .copy(isForWarningOnly = isForWarningOnly || state.isWarning)
            )
        }
    }

    private fun resolveDefaultAnnotationState(annotation: TAnnotation): ReportLevel {
        val annotationFqname = annotation.fqName
        if (annotationFqname != null && annotationFqname in JSPECIFY_DEFAULT_ANNOTATIONS) {
            return javaTypeEnhancementState.getReportLevelForAnnotation(annotationFqname)
        }
        return resolveJsr305AnnotationState(annotation)
    }

    // We explicitly state that while JSR-305 TYPE_USE annotations effectively should be applied to every type.
    // They are not applicable for type parameter bounds because it would be a breaking change otherwise.
    private fun Set<AnnotationQualifierApplicabilityType>.allIfTypeUse(): Set<AnnotationQualifierApplicabilityType> =
        if (AnnotationQualifierApplicabilityType.TYPE_USE in this)
            AnnotationQualifierApplicabilityType.values().toSet() - AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS + this
        else
            this

    private fun resolveTypeQualifierDefaultAnnotation(annotation: TAnnotation): TypeQualifierWithApplicability<TAnnotation>? {
        if (javaTypeEnhancementState.jsr305.isDisabled) return null
        val typeQualifierDefault = annotation.findAnnotation(JAVAX_TYPE_QUALIFIER_DEFAULT_ANNOTATION_FQ_NAME) ?: return null
        val typeQualifier = annotation.metaAnnotations.firstOrNull { resolveTypeQualifierAnnotation(it) != null } ?: return null
        val applicability = typeQualifierDefault.enumArguments(onlyValue = true)
            .mapNotNullTo(mutableSetOf()) { JAVA_APPLICABILITY_TYPES[it] }
        return TypeQualifierWithApplicability(typeQualifier, applicability.allIfTypeUse())
    }

    fun isTypeUseAnnotation(annotation: TAnnotation): Boolean {
        // Expect that Java's Target was mapped to Kotlin's Target.
        val target = annotation.findAnnotation(StandardNames.FqNames.target) ?: return false
        return target.enumArguments(onlyValue = false).any { it == KotlinTarget.TYPE.name }
    }

    fun shouldPropagateNullability(annotation: TAnnotation): Boolean {
        return when (annotation.fqName) {
            JSPECIFY_NON_NULL_ANNOTATION_FQ_NAME, JSPECIFY_NULLABLE_ANNOTATION_FQ_NAME -> false
            else -> true
        }
    }

    fun isAnnotationApplicableFromContainer(annotation: TAnnotation): Boolean {
        val fqName = annotation.fqName ?: return true
        return fqName != JSPECIFY_NON_NULL_ANNOTATION_FQ_NAME &&
                fqName != JSPECIFY_NULLABLE_ANNOTATION_FQ_NAME
    }

    private fun resolveJsr305AnnotationState(annotation: TAnnotation): ReportLevel {
        resolveJsr305CustomState(annotation)?.let { return it }
        return javaTypeEnhancementState.jsr305.globalLevel
    }

    private fun resolveJsr305CustomState(annotation: TAnnotation): ReportLevel? {
        javaTypeEnhancementState.jsr305.userDefinedLevelForSpecificAnnotation[annotation.fqName]?.let { return it }
        val enumValue = annotation.findAnnotation(UNDER_MIGRATION_ANNOTATION_FQ_NAME)?.enumArguments(onlyValue = false)?.firstOrNull()
            ?: return null
        return javaTypeEnhancementState.jsr305.migrationLevel ?: when (enumValue) {
            "STRICT" -> ReportLevel.STRICT
            "WARN" -> ReportLevel.WARN
            "IGNORE" -> ReportLevel.IGNORE
            else -> null
        }
    }

    private fun extractNullability(
        annotation: TAnnotation, forceWarning: TAnnotation.() -> Boolean
    ): NullabilityQualifierWithMigrationStatus? {
        knownNullability(annotation, annotation.forceWarning())?.let { return it }

        val typeQualifierAnnotation = resolveTypeQualifierAnnotation(annotation) ?: return null
        val jsr305State = resolveJsr305AnnotationState(annotation)
        if (jsr305State.isIgnore) return null
        // TODO: the result of `forceWarning` will be overwritten - expected? Probably not.
        return knownNullability(typeQualifierAnnotation, typeQualifierAnnotation.forceWarning())
            ?.copy(isForWarningOnly = jsr305State.isWarning)
    }

    fun extractNullability(
        annotations: Iterable<TAnnotation>, forceWarning: TAnnotation.() -> Boolean = { false }
    ): NullabilityQualifierWithMigrationStatus? =
        annotations.fold(null as NullabilityQualifierWithMigrationStatus?) { found, annotation ->
            val extracted = extractNullability(annotation, forceWarning)
            when {
                found == null -> extracted
                extracted == null || extracted == found -> found
                extracted.isForWarningOnly && !found.isForWarningOnly -> found
                !extracted.isForWarningOnly && found.isForWarningOnly -> extracted
                else -> return null // inconsistent
            }
        }

    fun extractMutability(annotations: Iterable<TAnnotation>): MutabilityQualifier? =
        annotations.fold(null as MutabilityQualifier?) { found, annotation ->
            when (annotation.fqName) {
                in READ_ONLY_ANNOTATIONS -> MutabilityQualifier.READ_ONLY
                in MUTABLE_ANNOTATIONS -> MutabilityQualifier.MUTABLE
                else -> return@fold found
            }.also { if (found != null && found != it) return null /* inconsistent */ }
        }

    private fun extractDefaultQualifiers(annotation: TAnnotation, isForWarningOnly: Boolean): JavaDefaultQualifiers? {
        resolveQualifierBuiltInDefaultAnnotation(annotation, isForWarningOnly)?.let { return it }

        val (typeQualifier, applicability) = resolveTypeQualifierDefaultAnnotation(annotation)
            ?: return null
        val jsr305State = resolveJsr305CustomState(annotation) ?: resolveJsr305AnnotationState(typeQualifier)
        if (jsr305State.isIgnore) return null
        // TODO: since we override the warning status, whether we force it in `extractNullability` is irrelevant.
        //   However, this is probably not what was intended.
        val nullabilityQualifier = extractNullability(typeQualifier) { false } ?: return null
        return JavaDefaultQualifiers(nullabilityQualifier.copy(isForWarningOnly = isForWarningOnly || jsr305State.isWarning), applicability)
    }

    fun extractAndMergeDefaultQualifiers(
        oldQualifiers: JavaTypeQualifiersByElementType?,
        annotations: Iterable<TAnnotation>,
        isForWarningOnly: Boolean = false,
    ): JavaTypeQualifiersByElementType? {
        if (javaTypeEnhancementState.disabledDefaultAnnotations) return oldQualifiers

        val extractedQualifiers = annotations.mapNotNull { extractDefaultQualifiers(it, isForWarningOnly) }
        if (extractedQualifiers.isEmpty()) return oldQualifiers

        val newQualifiers = QualifierByApplicabilityType(AnnotationQualifierApplicabilityType::class.java)

        // filter out inconsistencies in extracted qualifiers per applicability type
        for (extractedQualifier in extractedQualifiers) {
            for (applicabilityType in extractedQualifier.qualifierApplicabilityTypes) {
                if (applicabilityType !in newQualifiers || !isK2) {
                    // no qualifiers for applicabilityType were previously considered
                    // Or, in K1 mode, we just use the last qualifier
                    newQualifiers[applicabilityType] = extractedQualifier
                } else {
                    // one+ qualifiers for applicabilityType were already considered
                    val preexistingQualifier = newQualifiers[applicabilityType]
                        ?: continue // an inconsistency was already found when considering previous qualifiers
                    val preexistingNullability = preexistingQualifier.nullabilityQualifier
                    val extractedNullability = extractedQualifier.nullabilityQualifier
                    newQualifiers[applicabilityType] = when {
                        extractedNullability == preexistingNullability -> preexistingQualifier
                        extractedNullability.isForWarningOnly && !preexistingNullability.isForWarningOnly -> preexistingQualifier
                        !extractedNullability.isForWarningOnly && preexistingNullability.isForWarningOnly -> extractedQualifier
                        else -> null // qualifiers are inconsistent
                    }
                }
            }
        }

        val defaultQualifiersByType =
            oldQualifiers?.defaultQualifiers?.let(::QualifierByApplicabilityType)
                ?: QualifierByApplicabilityType(AnnotationQualifierApplicabilityType::class.java)

        var wasUpdate = false
        for ((applicabilityType, newQualifier) in newQualifiers) {
            if (newQualifier == null) continue // ignore inconsistent qualifiers
            defaultQualifiersByType[applicabilityType] = newQualifier
            wasUpdate = true
        }

        return if (!wasUpdate) oldQualifiers else JavaTypeQualifiersByElementType(defaultQualifiersByType)
    }

    private fun knownNullability(annotation: TAnnotation, forceWarning: Boolean): NullabilityQualifierWithMigrationStatus? {
        val fqName = annotation.fqName ?: return null
        val reportLevel = javaTypeEnhancementState.getReportLevelForAnnotation(fqName)
        if (reportLevel.isIgnore) return null
        val nullability = when (fqName) {
            in NOT_NULL_ANNOTATIONS -> NullabilityQualifier.NOT_NULL
            in NULLABLE_ANNOTATIONS -> NullabilityQualifier.NULLABLE
            in FORCE_FLEXIBILITY_ANNOTATIONS -> NullabilityQualifier.FORCE_FLEXIBILITY
            JAVAX_NONNULL_ANNOTATION_FQ_NAME ->
                when (annotation.enumArguments(onlyValue = false).firstOrNull()) {
                    "ALWAYS", null -> NullabilityQualifier.NOT_NULL
                    "MAYBE", "NEVER" -> NullabilityQualifier.NULLABLE
                    "UNKNOWN" -> NullabilityQualifier.FORCE_FLEXIBILITY
                    else -> return null
                }
            else -> return null
        }
        return NullabilityQualifierWithMigrationStatus(nullability, reportLevel.isWarning || forceWarning)
    }

    private companion object {
        val JAVA_APPLICABILITY_TYPES = mutableMapOf<String, AnnotationQualifierApplicabilityType>().apply {
            for (type in AnnotationQualifierApplicabilityType.values()) {
                getOrPut(type.javaTarget) { type }
            }
        }
    }
}

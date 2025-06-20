/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.SpecialNames

object FirJvmStaticChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirConstructor) {
            // WRONG_DECLARATION_TARGET
            return
        }

        if (declaration is FirPropertyAccessor) {
            return
        }

        fun checkIfAnnotated(it: FirDeclaration) {
            val annotation = it.findAnnotation(JvmStandardClassIds.Annotations.JvmStatic, context.session) ?: return
            val targetSource = annotation.source ?: it.source ?: declaration.source
            checkAnnotated(it, targetSource, declaration as? FirProperty)
        }

        checkIfAnnotated(declaration)
        if (declaration is FirProperty) {
            declaration.getter?.let { checkIfAnnotated(it) }
            declaration.setter?.let { checkIfAnnotated(it) }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotated(
        declaration: FirDeclaration,
        targetSource: KtSourceElement?,
        outerProperty: FirProperty? = null,
    ) {
        if (declaration !is FirMemberDeclaration) {
            return
        }

        val container = context.getContainerAt(0) ?: return
        val containerIsAnonymous = container is FirClassSymbol && container.classId.shortClassName == SpecialNames.ANONYMOUS

        if (
            container !is FirClassSymbol ||
            container.classKind != ClassKind.OBJECT ||
            !container.isCompanion() && containerIsAnonymous
        ) {
            reporter.reportOn(targetSource, FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION)
        } else if (
            container.isCompanion() &&
            context.containerIsInterface(1)
        ) {
            checkForInterface(declaration, targetSource)
        }

        checkOverrideCannotBeStatic(declaration, targetSource, outerProperty)
        checkStaticOnConstOrJvmField(declaration, targetSource)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkForInterface(
        declaration: FirDeclaration,
        targetSource: KtSourceElement?,
    ) {
        if (declaration !is FirCallableDeclaration) {
            return
        }

        val visibility = if (declaration is FirProperty) {
            declaration.getMinimumVisibility()
        } else {
            declaration.visibility
        }

        val isExternal = if (declaration is FirProperty) {
            declaration.hasExternalParts()
        } else {
            declaration.isExternal
        }

        if (visibility != Visibilities.Public) {
            reporter.reportOn(targetSource, FirJvmErrors.JVM_STATIC_ON_NON_PUBLIC_MEMBER)
        } else if (isExternal) {
            reporter.reportOn(targetSource, FirJvmErrors.JVM_STATIC_ON_EXTERNAL_IN_INTERFACE)
        }
    }

    private fun FirProperty.hasExternalParts(): Boolean {
        var hasExternal = isExternal

        getter?.let {
            hasExternal = hasExternal || it.isExternal
        }

        setter?.let {
            hasExternal = hasExternal || it.isExternal
        }

        return hasExternal
    }

    private fun FirProperty.getMinimumVisibility(): Visibility {
        var minVisibility = visibility

        getter?.let {
            minVisibility = chooseMostSpecific(minVisibility, it.visibility)
        }

        setter?.let {
            minVisibility = chooseMostSpecific(minVisibility, it.visibility)
        }

        return minVisibility
    }

    private fun chooseMostSpecific(a: Visibility, b: Visibility): Visibility {
        val difference = a.compareTo(b) ?: return a
        return if (difference > 0) {
            b
        } else {
            a
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOverrideCannotBeStatic(
        declaration: FirMemberDeclaration,
        targetSource: KtSourceElement?,
        outerProperty: FirProperty? = null,
    ) {
        val isOverride = outerProperty?.isOverride ?: declaration.isOverride

        if (!isOverride || !context.containerIsNonCompanionObject(0)) {
            return
        }

        reporter.reportOn(targetSource, FirJvmErrors.OVERRIDE_CANNOT_BE_STATIC)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticOnConstOrJvmField(
        declaration: FirDeclaration,
        targetSource: KtSourceElement?,
    ) {
        if (declaration !is FirProperty) return
        if (declaration.isConst || declaration.backingField?.hasAnnotationNamedAs(
                JvmStandardClassIds.Annotations.JvmField,
                context.session
            ) == true
        ) {
            reporter.reportOn(targetSource, FirJvmErrors.JVM_STATIC_ON_CONST_OR_JVM_FIELD)
        }
    }

    private fun CheckerContext.containerIsInterface(outerLevel: Int): Boolean {
        val container = this.getContainerAt(outerLevel)
        return container is FirClassSymbol && container.classKind.isInterface
    }

    private fun CheckerContext.containerIsNonCompanionObject(outerLevel: Int): Boolean {
        val containingClassSymbol = this.getContainerAt(outerLevel) ?: return false
        val containingClass = (containingClassSymbol as? FirRegularClassSymbol) ?: return false
        return containingClass.classKind == ClassKind.OBJECT && !containingClass.isCompanion
    }

    private fun CheckerContext.getContainerAt(outerLevel: Int): FirBasedSymbol<*>? {
        val correction = if (this.containingDeclarations.lastOrNull() is FirPropertySymbol) {
            1
        } else {
            0
        }
        return this.containingDeclarations.asReversed().getOrNull(outerLevel + correction)
    }

    private fun FirClassLikeSymbol<*>.isCompanion() = (this as? FirRegularClassSymbol)?.isCompanion == true

    private fun FirDeclaration.hasAnnotationNamedAs(classId: ClassId, session: FirSession): Boolean {
        return findAnnotation(classId, session) != null
    }

    private fun FirDeclaration.findAnnotation(classId: ClassId, session: FirSession): FirAnnotation? {
        return annotations.firstOrNull {
            it.annotationTypeRef.coneType.fullyExpandedClassId(session) == classId
        }
    }
}

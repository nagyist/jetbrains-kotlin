/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.computeTypeAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class StubBasedFirTypeDeserializer(
    private val moduleData: FirModuleData,
    private val annotationDeserializer: StubBasedAnnotationDeserializer,
    private val parent: StubBasedFirTypeDeserializer?,
    private val containingSymbol: FirBasedSymbol<*>?,
    owner: KtTypeParameterListOwner?,
    initialOrigin: FirDeclarationOrigin
) {
    private val typeParametersByName: Map<String, FirTypeParameterSymbol>

    val ownTypeParameters: List<FirTypeParameterSymbol>
        get() = typeParametersByName.values.toList()

    init {
        val typeParameters = owner?.typeParameters
        if (!typeParameters.isNullOrEmpty()) {
            typeParametersByName = mutableMapOf()
            val builders = mutableListOf<FirTypeParameterBuilder>()
            for (typeParameter in typeParameters) {
                val name = typeParameter.nameAsSafeName
                val symbol = FirTypeParameterSymbol().also {
                    typeParametersByName[name.asString()] = it
                }

                builders += FirTypeParameterBuilder().apply {
                    source = KtRealPsiSourceElement(typeParameter)
                    moduleData = this@StubBasedFirTypeDeserializer.moduleData
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = initialOrigin
                    this.name = name
                    this.symbol = symbol
                    this.containingDeclarationSymbol = containingSymbol ?: errorWithAttachment("Top-level type parameter ???") {
                        withPsiEntry("owner", owner)
                        withPsiEntry("parameter", typeParameter)
                    }

                    variance = typeParameter.variance
                    isReified = typeParameter.hasModifier(KtTokens.REIFIED_KEYWORD)
                    annotations += annotationDeserializer.loadAnnotations(
                        ktAnnotated = typeParameter,
                        useSiteTargetFilter = StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
                    )
                }
            }

            for ((index, typeParameter) in typeParameters.withIndex()) {
                val builder = builders[index]
                builder.apply {
                    typeParameter.extendsBound?.let { bounds.add(typeRef(it)) }
                    owner.typeConstraints
                        .filter { it.subjectTypeParameterName?.getReferencedNameAsName() == typeParameter.nameAsName }
                        .forEach { typeConstraint -> typeConstraint.boundTypeReference?.let { bounds += typeRef(it) } }
                    addDefaultBoundIfNecessary()
                }.build()
            }
        } else {
            typeParametersByName = emptyMap()
        }
    }

    fun typeRef(typeReference: KtTypeReference): FirTypeRef = buildResolvedTypeRef {
        source = KtRealPsiSourceElement(typeReference)
        annotations += annotationDeserializer.loadAnnotations(
            ktAnnotated = typeReference,
            useSiteTargetFilter = StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
        )

        coneType = type(typeReference, annotations.computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false))
    }

    fun type(typeReference: KtTypeReference): ConeKotlinType {
        val annotations = annotationDeserializer.loadAnnotations(
            typeReference,
            StubBasedAnnotationDeserializer.TYPE_ANNOTATIONS_FILTER,
        ).toMutableList()

        val parent = (typeReference.stub ?: loadStubByElement(typeReference))?.parentStub
        if (parent is KotlinParameterStubImpl) {
            parent.functionTypeParameterName?.let { paramName ->
                annotations += buildAnnotation {
                    annotationTypeRef = buildResolvedTypeRef {
                        coneType = StandardNames.FqNames.parameterNameClassId.toLookupTag()
                            .constructClassType()
                    }
                    this.argumentMapping = buildAnnotationArgumentMapping {
                        mapping[StandardNames.NAME] =
                            buildLiteralExpression(null, ConstantValueKind.String, paramName, setType = true)
                    }
                }
            }
        }
        return type(typeReference, annotations.computeTypeAttributes(moduleData.session, shouldExpandTypeAliases = false))
    }

    fun type(type: KotlinTypeBean): ConeKotlinType? {
        when (type) {
            is KotlinTypeParameterTypeBean -> {
                val lookupTag =
                    typeParametersByName[type.typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(type.typeParameterName)
                    ?: return null
                return ConeTypeParameterTypeImpl(lookupTag, isMarkedNullable = type.nullable).let {
                    if (type.definitelyNotNull)
                        ConeDefinitelyNotNullType.create(it, moduleData.session.typeContext, avoidComprehensiveCheck = true) ?: it
                    else
                        it
                }
            }
            is KotlinClassTypeBean -> {
                return deserializeClassType(type)
            }
            is KotlinFlexibleTypeBean -> {
                val lowerBound = type(type.lowerBound).asRigidType
                val upperBound = type(type.upperBound).asRigidType
                return ConeFlexibleType(lowerBound, upperBound, isTrivial = false)
            }
        }
    }

    private val ConeKotlinType?.asRigidType: ConeRigidType
        get() = when (this) {
            is ConeRigidType -> this
            null, is ConeFlexibleType -> errorWithAttachment("Unexpected cone type ${this?.let { it::class.simpleName }}") {
                withConeTypeEntry("bound", this@asRigidType)
            }
        }

    private fun deserializeClassType(typeBean: KotlinClassTypeBean): ConeClassLikeType {
        val projections = typeBean.arguments.map { typeArgumentBean ->
            val kind = typeArgumentBean.projectionKind
            if (kind == KtProjectionKind.STAR) {
                return@map ConeStarProjection
            }
            val argBean = typeArgumentBean.type!!
            val lowerBound = type(argBean)
                ?: errorWithAttachment("Broken type argument ${typeArgumentBean.type?.let { it::class }}") {
                    withEntry("type", typeArgumentBean.type) { it.toString() }
                }
            typeArgument(lowerBound, kind)
        }

        val abbreviatedTypeAttribute = typeBean.abbreviatedType?.let { AbbreviatedTypeAttribute(deserializeClassType(it)) }
        val attributes = ConeAttributes.create(listOfNotNull(abbreviatedTypeAttribute))

        return ConeClassLikeTypeImpl(
            typeBean.classId.toLookupTag(),
            projections.toTypedArray(),
            isMarkedNullable = typeBean.nullable,
            attributes,
        )
    }

    private fun type(typeReference: KtTypeReference, attributes: ConeAttributes): ConeKotlinType {
        val unwrappedTypeElement = typeReference.typeElement?.unwrapNullability()
        if (unwrappedTypeElement is KtDynamicType) {
            return ConeDynamicType.create(moduleData.session)
        }

        return when (unwrappedTypeElement) {
            is KtFunctionType -> deserializeFunctionType(typeReference, unwrappedTypeElement, attributes)
            is KtUserType -> deserializeUserType(typeReference, unwrappedTypeElement, attributes)
            else -> simpleTypeOrError(typeReference, attributes)
        }
    }

    private fun deserializeFunctionType(typeReference: KtTypeReference, type: KtFunctionType, attributes: ConeAttributes): ConeKotlinType {
        val stub = (type.stub ?: loadStubByElement(type)) as? KotlinFunctionTypeStubImpl
        return simpleTypeOrError(typeReference, attributes.withAbbreviation(stub?.abbreviatedType))
    }

    private fun deserializeUserType(typeReference: KtTypeReference, type: KtUserType, attributes: ConeAttributes): ConeKotlinType {
        val stub = (type.stub ?: loadStubByElement(type)) as? KotlinUserTypeStubImpl
        val coneType = simpleTypeOrError(typeReference, attributes.withAbbreviation(stub?.abbreviatedType))

        val upperBoundTypeBean = stub?.upperBound
        return if (upperBoundTypeBean != null) {
            val upperBoundType = type(upperBoundTypeBean)

            // If an upper bound is specified, `typeReference` represents a flexible type. The cone type deserialized from `typeReference`
            // is defined as the lower bound of this flexible type.
            ConeFlexibleType(coneType, upperBoundType as ConeSimpleKotlinType, isTrivial = false)
        } else {
            coneType
        }
    }

    private fun ConeAttributes.withAbbreviation(abbreviatedType: KotlinClassTypeBean?): ConeAttributes {
        if (abbreviatedType == null) return this
        return add(AbbreviatedTypeAttribute(deserializeClassType(abbreviatedType)))
    }

    private fun typeParameterSymbol(typeParameterName: String): ConeTypeParameterLookupTag? =
        typeParametersByName[typeParameterName]?.toLookupTag() ?: parent?.typeParameterSymbol(typeParameterName)

    fun FirClassLikeSymbol<*>.typeParameters(): List<FirTypeParameterSymbol> =
        (fir as? FirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol }.orEmpty()

    private fun simpleType(typeReference: KtTypeReference, attributes: ConeAttributes): ConeRigidType? {
        val constructor = typeSymbol(typeReference) ?: return null
        val isNullable = typeReference.typeElement is KtNullableType
        if (constructor is ConeTypeParameterLookupTag) {
            return ConeTypeParameterTypeImpl(constructor, isMarkedNullable = isNullable, attributes).let {
                if (typeReference.typeElement?.unwrapNullability() is KtIntersectionType) {
                    ConeDefinitelyNotNullType.create(it, moduleData.session.typeContext, avoidComprehensiveCheck = true) ?: it
                } else it
            }
        }
        if (constructor !is ConeClassLikeLookupTag) return null

        val typeElement = typeReference.typeElement?.unwrapNullability()
        val arguments = when (typeElement) {
            is KtUserType -> buildList {
                // The type for Outer<T>.Inner<S> needs to have type args <S, T>
                var current: KtUserType? = typeElement
                while (current != null) {
                    current.typeArguments.forEach { add(typeArgument(it)) }
                    current = current.qualifier
                }
            }.toTypedArray()
            is KtFunctionType -> buildList {
                typeElement.contextReceiversTypeReferences.mapTo(this) { type(it).toTypeProjection(Variance.INVARIANT) }
                typeElement.receiver?.let { add(type(it.typeReference).toTypeProjection(Variance.INVARIANT)) }
                typeElement.parameters.mapTo(this) { type(it.typeReference!!).toTypeProjection(Variance.INVARIANT) }
                add(type(typeElement.returnTypeReference!!).toTypeProjection(Variance.INVARIANT))
            }.toTypedArray()
            else -> errorWithAttachment("not supported ${typeElement?.let { it::class }}") {
                withPsiEntry("typeElement", typeElement)
            }
        }

        return ConeClassLikeTypeImpl(
            constructor,
            arguments,
            isMarkedNullable = isNullable,
            attributes.withExtensionFunctionTypeIfNeeded(typeElement).withContextParametersFunctionTypeIfNeeded(typeElement),
        )
    }

    private fun ConeAttributes.withExtensionFunctionTypeIfNeeded(typeElement: KtTypeElement): ConeAttributes {
        if (typeElement !is KtFunctionType) return this
        if (typeElement.receiver == null) return this
        return add(ConeAttributes.WithExtensionFunctionType)
    }

    private fun ConeAttributes.withContextParametersFunctionTypeIfNeeded(typeElement: KtTypeElement): ConeAttributes {
        if (typeElement !is KtFunctionType) return this
        val contextParametersCount = typeElement.contextReceiverList?.contextReceivers()?.size ?: 0
        if (contextParametersCount <= 0) return this

        return add(CompilerConeAttributes.ContextFunctionTypeParams(contextParametersCount))
    }

    private fun simpleTypeOrError(typeReference: KtTypeReference, attributes: ConeAttributes): ConeRigidType =
        simpleType(typeReference, attributes) ?: ConeErrorType(ConeSimpleDiagnostic("?!id:0", DiagnosticKind.DeserializationError))

    private fun KtFunctionType.isSuspend(): Boolean {
        val parent = parent as? KtElementImplStub<*>
            ?: error("Expected parent of KtTypeElement to have type KtElementImplStub<*>, but actual $parent")
        val modifiers = parent.getStubOrPsiChildren(KtStubElementTypes.MODIFIER_LIST, KtStubElementTypes.MODIFIER_LIST.arrayFactory)
        return modifiers.any { it.hasSuspendModifier() }
    }

    private fun typeSymbol(typeReference: KtTypeReference): ConeClassifierLookupTag? {
        val typeElement = typeReference.typeElement?.unwrapNullability()
        if (typeElement is KtFunctionType) {
            val arity = typeElement.totalParameterCount
            val isSuspend = typeElement.isSuspend()
            val functionClassId = when {
                isSuspend -> StandardNames.getSuspendFunctionClassId(arity)

                /*
                 * Since 2.1 any `@Composable FunctionN` type is serialized to metadata as `ComposableFunctionN`, which is consistent with
                 * how composable functions are treated in sources (with compose plugin enabled). But there are old libraries compiled
                 * with 2.0 or less, which still have `@Composable FunctionN` types. To handle such libraries in the CLI compiler plugins
                 * are passed to the library session so they could be applied to deserialized classes.
                 * But it's impossible to do the same in the IDE, because there libraries don't know anything about source modules they
                 * will be used in. So to work around this issue this conversion for Composable functions is hardcoded
                 */
                typeReference.annotationEntries.any {
                    StubBasedAnnotationDeserializer.getAnnotationClassId(it) == composableClassId
                } -> getComposableFunctionClassId(arity)

                else -> StandardNames.getFunctionClassId(arity)
            }
            return functionClassId.toLookupTag()
        }
        if (typeElement is KtIntersectionType) {
            val leftTypeRef = typeElement.getLeftTypeRef() ?: return null
            //T&Any
            return typeSymbol(leftTypeRef)
        }
        val type = typeElement as KtUserType
        val referencedName = type.referencedName
        return typeParameterSymbol(referencedName!!) ?: type.classId().toLookupTag()
    }

    private fun getComposableFunctionClassId(arity: Int): ClassId {
        val name = Name.identifier("$composableFunctionPrefix$arity")
        return ClassId(internalComposePackageFqName, name)
    }


    private fun typeArgument(projection: KtTypeProjection): ConeTypeProjection {
        if (projection.projectionKind == KtProjectionKind.STAR) {
            return ConeStarProjection
        }

        val type = type(projection.typeReference!!)
        return typeArgument(type, projection.projectionKind)
    }

    private fun typeArgument(
        type: ConeKotlinType,
        projectionKind: KtProjectionKind
    ): ConeTypeProjection {
        val variance = when (projectionKind) {
            KtProjectionKind.IN -> Variance.IN_VARIANCE
            KtProjectionKind.OUT -> Variance.OUT_VARIANCE
            KtProjectionKind.NONE -> Variance.INVARIANT
            KtProjectionKind.STAR -> throw AssertionError("* should not be here")
        }
        return type.toTypeProjection(variance)
    }

    companion object {
        private val internalComposePackageFqName = FqName("androidx.compose.runtime.internal")
        private val composableClassId = ClassId(
            FqName("androidx.compose.runtime"),
            Name.identifier("Composable"),
        )
        private val composableFunctionPrefix = "ComposableFunction"
    }
}

/**
 * Retrieves classId from [KtUserType] for compiled code only.
 *
 * It relies on [org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl.isClassRef],
 * which is set during cls analysis only.
 */
internal fun KtUserType.classId(): ClassId {
    val packageFragments = mutableListOf<String>()
    val classFragments = mutableListOf<String>()

    fun collectFragments(type: KtUserType) {
        val userType = type.getStubOrPsiChild(KtStubElementTypes.USER_TYPE)
        if (userType != null) {
            collectFragments(userType)
        }
        val referenceExpression = type.referenceExpression as? KtNameReferenceExpression
        if (referenceExpression != null) {
            val referencedName = referenceExpression.getReferencedName()
            val stub = referenceExpression.stub ?: loadStubByElement(referenceExpression)
            if (stub is KotlinNameReferenceExpressionStubImpl && stub.isClassRef) {
                classFragments.add(referencedName)
            } else {
                packageFragments.add(referencedName)
            }
        }
    }
    collectFragments(this)
    return ClassId(
        FqName.fromSegments(packageFragments),
        FqName.fromSegments(classFragments),
        isLocal = false
    )
}

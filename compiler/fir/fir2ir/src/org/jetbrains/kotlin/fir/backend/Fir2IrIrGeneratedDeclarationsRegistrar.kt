/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrGeneratedDeclarationsRegistrar
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.compilerPluginMetadata
import org.jetbrains.kotlin.fir.deserialization.toResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildUnaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.serialization.FirAdditionalMetadataProvider
import org.jetbrains.kotlin.fir.serialization.providedDeclarationsForMetadataService
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind

// opt-in is safe, this code runs after fir2ir is over and all symbols are bound
@OptIn(UnsafeDuringIrConstructionAPI::class)
class Fir2IrIrGeneratedDeclarationsRegistrar(private val components: Fir2IrComponents) : IrGeneratedDeclarationsRegistrar() {
    private val session: FirSession
        get() = components.session

    private val implicitType: FirImplicitTypeRef
        get() = FirImplicitTypeRefImplWithoutSource

    private val annotationsStorage = mutableMapOf<FirDeclaration, MutableList<IrConstructorCall>>()
    private val annotationsOnParametersStorage = mutableMapOf<FirDeclaration, MutableMap<ChildDeclarationKind, MutableList<IrConstructorCall>>>()

    private sealed class ChildDeclarationKind {
        data class ValueParameter(val name: Name) : ChildDeclarationKind()
        data class TypeParameter(val name: Name) : ChildDeclarationKind()
    }

    override fun addMetadataVisibleAnnotationsToElement(declaration: IrDeclaration, annotations: List<IrConstructorCall>) {
        require(declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
            "FAKE_OVERRIDE declarations are not preserved in metadata and should not be marked with annotations: ${declaration.render()}"
        }
        require(annotations.all { it.typeArguments.isEmpty() }) {
            "Saving annotations with type arguments from IR to metadata is not supported: ${declaration.render()}"
        }
        annotations.forEach {
            require(it.symbol.owner.constructedClass.isAnnotationClass) { "${it.render()} is not an annotation constructor call" }
        }
        val (firDeclaration, kind) = findFirDeclaration(declaration)

        when (kind) {
            null -> annotationsStorage.getOrPut(firDeclaration) { mutableListOf() } += annotations
            else -> {
                val storageForDeclaration = annotationsOnParametersStorage.getOrPut(firDeclaration) { mutableMapOf() }
                storageForDeclaration.getOrPut(kind) { mutableListOf() } += annotations
            }
        }
        declaration.annotations += annotations
    }

    private fun findFirDeclaration(declaration: IrDeclaration): Pair<FirDeclaration, ChildDeclarationKind?> {
        return when (declaration) {
            is IrMetadataSourceOwner -> {
                val firDeclaration = (declaration.metadata as? FirMetadataSource)?.fir
                    ?: error("Fir declaration is not found for ${declaration.render()}")
                firDeclaration to null
            }
            is IrValueParameter -> findFirDeclaration(declaration.parent as IrDeclaration).first to ChildDeclarationKind.ValueParameter(declaration.name)
            is IrTypeParameter -> findFirDeclaration(declaration.parent as IrDeclaration).first to ChildDeclarationKind.TypeParameter(declaration.name)
            else -> error("Declaration with annotations should be `IrMetadataSourceOwner`, `IrValueParameter` or `IrTypeParameter`, but got ${declaration.render()}")
        }
    }

    override fun registerFunctionAsMetadataVisible(irFunction: IrSimpleFunction) {
        if (irFunction.isLocal || irFunction.parentClassOrNull?.isLocal == true) return
        val firFunction = buildSimpleFunction {
            moduleData = session.moduleData
            origin = GeneratedForMetadata.origin
            status = FirResolvedDeclarationStatusImpl(
                irFunction.visibility.delegate,
                irFunction.modality,
                irFunction.visibility.delegate.toEffectiveVisibility(owner = null)
            ).apply {
                isExpect = irFunction.isExpect
                isActual = false
                isOverride = irFunction.overriddenSymbols.isNotEmpty()
                isInfix = irFunction.isInfix
                isInline = irFunction.isInline
                isTailRec = irFunction.isTailrec
                isSuspend = irFunction.isSuspend
            }
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            returnTypeRef = implicitType
            dispatchReceiverType = irFunction.parent.toFirClass()?.defaultType()
            // contextReceivers
            // valueParameters
            name = irFunction.name
            symbol = FirNamedFunctionSymbol(irFunction.callableId)
            // annotations
            irFunction.typeParameters.mapTo(typeParameters) {
                buildTypeParameter {
                    moduleData = session.moduleData
                    origin = GeneratedForMetadata.origin
                    name = it.name
                    symbol = FirTypeParameterSymbol()
                    containingDeclarationSymbol = this@buildSimpleFunction.symbol
                    variance = it.variance
                    isReified = it.isReified
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    // bounds
                    // annotations
                }
            }
        }

        with(TypeConverter(irFunction, firFunction)) {
            updateFunctionCommon(firFunction, irFunction)

            with(firFunction) {
                for ((firParameter, irParameter) in typeParameters.zip(irFunction.typeParameters)) {
                    val newBounds = irParameter.superTypes.map { it.toConeType().toFirResolvedTypeRef() }
                    firParameter.replaceBounds(newBounds)
                    firParameter.replaceAnnotations(irParameter.convertAnnotations())
                }
            }
        }

        session.providedDeclarationsForMetadataService.registerDeclaration(firFunction)

        irFunction.metadata = FirMetadataSource.Function(firFunction)
    }

    override fun registerConstructorAsMetadataVisible(irConstructor: IrConstructor) {
        if (irConstructor.isLocal || irConstructor.parentAsClass.isLocal) return
        val constructedClass = irConstructor.parent.toFirClass()
            ?: error("Fir class for constructor ${irConstructor.render()} not found")
        val firConstructor = buildConstructor {
            moduleData = session.moduleData
            origin = GeneratedForMetadata.origin
            status = FirResolvedDeclarationStatusImpl(
                irConstructor.visibility.delegate,
                Modality.FINAL,
                irConstructor.visibility.delegate.toEffectiveVisibility(owner = null)
            ).apply {
                isExpect = irConstructor.isExpect
                isActual = false
            }
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            returnTypeRef = implicitType

            // contextReceivers
            // valueParameters
            symbol = FirConstructorSymbol(constructedClass.classId)
            // annotations
            constructedClass.typeParameters.mapTo(typeParameters) { buildConstructedClassTypeParameterRef { symbol = it.symbol } }
        }

        with(TypeConverter(irConstructor, firConstructor)) {
            updateFunctionCommon(firConstructor, irConstructor)
            firConstructor.containingClassForStaticMemberAttr = constructedClass.symbol.toLookupTag()
        }

        session.providedDeclarationsForMetadataService.registerDeclaration(firConstructor)

        irConstructor.metadata = FirMetadataSource.Function(firConstructor)
    }

    private fun TypeConverter.updateFunctionCommon(firFunction: FirFunction, irFunction: IrFunction) = with(firFunction) {
        replaceReturnTypeRef(irFunction.returnType.toConeType().toFirResolvedTypeRef())
        val contextParameters = mutableListOf<FirValueParameter>()
        val valueParameters = mutableListOf<FirValueParameter>()

        for (parameter in irFunction.parameters) {
            when (parameter.kind) {
                IrParameterKind.DispatchReceiver -> {} // dispatch receiver is handled separately
                IrParameterKind.ExtensionReceiver -> {
                    replaceReceiverParameter(buildReceiverParameter {
                        moduleData = session.moduleData
                        origin = GeneratedForMetadata.origin
                        symbol = FirReceiverParameterSymbol()
                        typeRef = parameter.type.toConeType().toFirResolvedTypeRef()
                        containingDeclarationSymbol = firFunction.symbol
                        annotations.addAll(parameter.convertAnnotations())
                    })
                }
                IrParameterKind.Regular, IrParameterKind.Context -> {
                    val isContext = parameter.kind == IrParameterKind.Context
                    buildValueParameter {
                        moduleData = session.moduleData
                        origin = GeneratedForMetadata.origin
                        returnTypeRef = parameter.type.toConeType().toFirResolvedTypeRef()
                        name = parameter.name
                        symbol = FirValueParameterSymbol()
                        if (parameter.defaultValue != null) {
                            defaultValue = buildExpressionStub {
                                coneTypeOrNull = this@buildValueParameter.returnTypeRef.coneType
                            }
                        }
                        containingDeclarationSymbol = firFunction.symbol
                        isCrossinline = parameter.isCrossinline
                        isNoinline = parameter.isNoinline
                        isVararg = parameter.isVararg
                        annotations.addAll(parameter.convertAnnotations())
                        resolvePhase = FirResolvePhase.BODY_RESOLVE
                        valueParameterKind = if (isContext) FirValueParameterKind.ContextParameter else FirValueParameterKind.Regular
                    }.also {
                        if (isContext) {
                            contextParameters += it
                        } else {
                            valueParameters += it
                        }
                    }
                }
            }
        }

        replaceValueParameters(valueParameters)
        replaceContextParameters(contextParameters)
        replaceAnnotations(irFunction.convertAnnotations())
    }

    fun createAdditionalMetadataProvider(): FirAdditionalMetadataProvider {
        return Provider()
    }

    private fun IrDeclarationParent.toFirClass(): FirRegularClass? {
        return ((this as? IrClass)?.metadata as? FirMetadataSource.Class)?.fir as? FirRegularClass
    }

    private fun IrAnnotationContainer.convertAnnotations(): List<FirAnnotation> {
        return this.annotations.map { it.toFirAnnotation() }
    }

    private open inner class TypeConverter(val originalFunction: IrFunction?, val convertedFunction: FirFunction?) {
        init {
            if (originalFunction != null && convertedFunction == null) {
                error("Conversion with null `convertedFunction`is unsupported")
            }
        }

        fun IrType.toConeType(): ConeKotlinType {
            return when (this) {
                is IrSimpleType -> {
                    val lookupTag = classifier.toLookupTag()
                    lookupTag.constructType(
                        this.arguments.map { it.toConeTypeProjection() }.toTypedArray(),
                        isMarkedNullable = this.isMarkedNullable()
                    )
                }
                is IrDynamicType -> ConeDynamicType.create(session)
                else -> error("Unsupported IR type: $this")
            }
        }

        private fun IrTypeArgument.toConeTypeProjection(): ConeTypeProjection {
            return when (this) {
                is IrStarProjection -> ConeStarProjection
                is IrTypeProjection -> type.toConeType().toTypeProjection(variance)
            }
        }

        private fun IrClassifierSymbol.toLookupTag(): ConeClassifierLookupTag {
            return when (val owner = owner) {
                is IrClass -> owner.classIdOrFail.toLookupTag()
                is IrTypeParameter -> {
                    val typeParameter = when (val parent = owner.parent) {
                        // guarded by init block, so !! is safe
                        originalFunction -> convertedFunction!!.typeParameters[owner.index]
                        is IrClass -> {
                            val firClass = parent.classIdOrFail.toLookupTag().toRegularClassSymbol(session)?.fir
                                ?: error("Fir class for ${parent.render()} not found")
                            firClass.typeParameters[owner.index]
                        }
                        else -> error("Unsupported type parameter container: ${parent.render()}")
                    }
                    typeParameter.symbol.toLookupTag()
                }
                else -> error("Unsupported IR classifier: ${owner.render()}")
            }
        }
    }

    private val emptyTypeConverter = TypeConverter(null, null)

    private fun IrExpression.toFirExpression(): FirExpression {
        return when (this) {
            is IrConst -> {
                when (this.kind) {
                    IrConstKind.Boolean -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Boolean,
                        this.value as Boolean,
                        setType = true
                    )
                    IrConstKind.Byte -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Byte,
                        this.value as Byte,
                        setType = true
                    )
                    IrConstKind.Char -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Char,
                        this.value as Char,
                        setType = true
                    )
                    IrConstKind.Double -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Double,
                        this.value as Double,
                        setType = true
                    )
                    IrConstKind.Float -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Float,
                        this.value as Float,
                        setType = true
                    )
                    IrConstKind.Int -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Int,
                        this.value as Int,
                        setType = true
                    )
                    IrConstKind.Long -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Long,
                        this.value as Long,
                        setType = true
                    )
                    IrConstKind.Null -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Null,
                        value = null,
                        setType = true
                    )
                    IrConstKind.Short -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.Short,
                        this.value as Short,
                        setType = true
                    )
                    IrConstKind.String -> buildLiteralExpression(
                        source = null,
                        ConstantValueKind.String,
                        this.value as String,
                        setType = true
                    )
                }
            }
            is IrGetEnumValue -> {
                val enumClassId = (this.symbol.owner.parent as IrClass).classId!!
                val enumVariantName = this.symbol.owner.name
                val enumEntrySymbol = session.symbolProvider.getClassLikeSymbolByClassId(enumClassId)?.let { classSymbol ->
                    (classSymbol as? FirRegularClassSymbol)?.declarationSymbols
                        ?.filterIsInstance<FirEnumEntrySymbol>()
                        ?.find { it.name == enumVariantName }
                } ?: error("Could not resolve FirEnumEntry for $enumClassId.$enumVariantName")

                buildPropertyAccessExpression {
                    val receiver = enumClassId.toResolvedQualifier(session)
                    coneTypeOrNull = receiver.resolvedType
                    calleeReference = buildResolvedNamedReference {
                        name = enumVariantName
                        resolvedSymbol = enumEntrySymbol
                    }
                    explicitReceiver = receiver
                    dispatchReceiver = receiver
                }
            }
            is IrConstructorCall -> this.toFirAnnotation()
            is IrVararg -> {
                val varargElements = this.elements.map { element ->
                    when (element) {
                        is IrExpression -> element.toFirExpression()
                        else -> error("Unsupported ir type: ${element.render()}")
                    }
                }

                with(emptyTypeConverter) {
                    val type = this@toFirExpression.type.toConeType()
                    val elemType = this@toFirExpression.varargElementType.toConeType()

                    buildVarargArgumentsExpression {
                        arguments.addAll(varargElements)
                        coneTypeOrNull = type
                        coneElementTypeOrNull = elemType
                    }
                }
            }
            is IrClassReference -> {
                buildGetClassCall {
                    with(emptyTypeConverter) {
                        val resolvedType = this@toFirExpression.type.toConeType()
                        coneTypeOrNull = resolvedType
                        argumentList = buildUnaryArgumentList(
                            buildClassReferenceExpression {
                                coneTypeOrNull = this@toFirExpression.classType.toConeType()
                                classTypeRef = buildResolvedTypeRef { coneType = resolvedType }
                            }
                        )
                    }
                }
            }
            else -> error("Unsupported ir type: ${this.render()}")
        }
    }

    private fun IrConstructorCall.toFirAnnotation(): FirAnnotation {
        val annotationClassId = this.symbol.owner.constructedClass.classId!!
        return buildAnnotation {
            annotationTypeRef = annotationClassId
                .toLookupTag()
                .constructClassType()
                .toFirResolvedTypeRef()
            argumentMapping = buildAnnotationArgumentMapping {
                for ((i, argument) in this@toFirAnnotation.arguments.withIndex()) {
                    if (argument == null) continue
                    val argName = this@toFirAnnotation.symbol.owner.parameters[i].name
                    this.mapping[argName] = argument.toFirExpression()
                }
            }
        }
    }

    private object GeneratedForMetadata : GeneratedDeclarationKey()

    private val metadataExtensionsForDeclarations: MutableMap<FirDeclaration, MutableMap<String, ByteArray>> = mutableMapOf()

    override fun addCustomMetadataExtension(
        irDeclaration: IrDeclaration,
        pluginId: String,
        data: ByteArray,
    ) {
        val metadataSource = (irDeclaration as? IrMetadataSourceOwner)?.metadata
            ?: error("No metadata source found for ${irDeclaration.render()}")
        val firDeclaration = (metadataSource as? FirMetadataSource)?.fir
            ?: error("No FIR declaration found for ${irDeclaration.render()}")
        val extensionsPerPlugin = metadataExtensionsForDeclarations.getOrPut(firDeclaration) { mutableMapOf() }
        val existed = extensionsPerPlugin.put(pluginId, data)
        require(existed == null) {
            "There is already metadata value for plugin $pluginId and ${irDeclaration.render()}"
        }
    }

    override fun getCustomMetadataExtension(
        irDeclaration: IrDeclaration,
        pluginId: String,
    ): ByteArray? {
        val firDeclaration = (irDeclaration as? AbstractFir2IrLazyDeclaration<*>)?.fir ?: return null
        return firDeclaration.compilerPluginMetadata?.get(pluginId)
    }

    private inner class Provider : FirAdditionalMetadataProvider() {
        override fun findGeneratedAnnotationsFor(declaration: FirDeclaration): List<FirAnnotation> {
            val irAnnotations = extractGeneratedIrDeclarations(declaration).takeUnless { it.isEmpty() } ?: return emptyList()
            return irAnnotations.map { it.toFirAnnotation() }
        }

        override fun hasGeneratedAnnotationsFor(declaration: FirDeclaration): Boolean {
            return extractGeneratedIrDeclarations(declaration).isNotEmpty()
        }

        private fun extractGeneratedIrDeclarations(declaration: FirDeclaration): List<IrConstructorCall> {
            when (declaration.origin) {
                is FirDeclarationOrigin.Synthetic,
                is FirDeclarationOrigin.Delegated
                    -> return emptyList()
                else -> {}
            }
            val (keyDeclaration, kind) = when (declaration) {
                is FirValueParameter -> declaration.containingDeclarationSymbol.fir to ChildDeclarationKind.ValueParameter(declaration.name)
                is FirTypeParameter -> declaration.containingDeclarationSymbol.fir to ChildDeclarationKind.TypeParameter(declaration.name)
                else -> declaration to null
            }
            return when (kind) {
                null -> annotationsStorage[keyDeclaration].orEmpty()
                else -> annotationsOnParametersStorage[keyDeclaration].orEmpty()[kind].orEmpty()
            }
        }

        override fun findMetadataExtensionsFor(declaration: FirDeclaration): Map<String, ByteArray> {
            return metadataExtensionsForDeclarations[declaration].orEmpty()
        }
    }
}

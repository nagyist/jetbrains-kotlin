/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrAbstractDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val DECLARATION_ORIGIN_FUNCTION_CLASS by IrDeclarationOriginImpl

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrPackageFragment.isFunctionInterfaceFile get() = packageFragmentDescriptor is FunctionInterfacePackageFragment

abstract class KonanIrAbstractDescriptorBasedFunctionFactory : IrProvider, IrAbstractDescriptorBasedFunctionFactory()

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class LazyIrFunctionFactory(
        private val symbolTable: SymbolTable,
        private val stubGenerator: DeclarationStubGenerator,
        private val irBuiltIns: IrBuiltInsOverDescriptors,
        private val reflectionTypes: KonanReflectionTypes
) : KonanIrAbstractDescriptorBasedFunctionFactory() {

    override fun getDeclaration(symbol: IrSymbol) =
            (symbol.descriptor as? FunctionClassDescriptor)?.let { descriptor ->
                buildClass(descriptor) {
                    descriptorExtension.declareClass(descriptor) {
                        createIrClass(descriptor)
                    }
                }
            }

    override fun functionClassDescriptor(arity: Int): FunctionClassDescriptor =
            irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor

    override fun kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            reflectionTypes.getKFunction(arity) as FunctionClassDescriptor

    override fun suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor

    override fun kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            reflectionTypes.getKSuspendFunction(arity) as FunctionClassDescriptor

    override fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor, declarator)

    override fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(reflectionTypes.getKFunction(arity) as FunctionClassDescriptor, declarator)

    override fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor, declarator)

    override fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(reflectionTypes.getKSuspendFunction(arity) as FunctionClassDescriptor, declarator)

    private val builtClassesMap = mutableMapOf<FunctionClassDescriptor, IrClass>()

    private fun createIrClass(descriptor: ClassDescriptor): IrClass =
            stubGenerator.generateClassStub(descriptor)

    private fun createClass(descriptor: FunctionClassDescriptor, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            symbolTable.declarator { createIrClass(descriptor) }

    private fun buildClass(descriptor: FunctionClassDescriptor, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            builtClassesMap.getOrPut(descriptor) { createClass(descriptor, declarator) }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class BuiltInFictitiousFunctionIrClassFactory(
        private val symbolTable: SymbolTable,
        private val irBuiltIns: IrBuiltInsOverDescriptors,
        private val reflectionTypes: KonanReflectionTypes
) : KonanIrAbstractDescriptorBasedFunctionFactory() {

    override fun getDeclaration(symbol: IrSymbol) =
            (symbol.descriptor as? FunctionClassDescriptor)?.let { descriptor ->
                buildClass(descriptor) {
                    descriptorExtension.declareClass(descriptor) {
                        createIrClass(it, descriptor)
                    }
                }
            }

    var module: IrModuleFragment? = null
        set(value) {
            if (value == null)
                error("Provide a valid non-null module")
            if (field != null)
                error("Module has already been set")
            field = value
            filesMap.values.forEach(value::addFile)
//            builtClasses.forEach { it.addFakeOverrides() }
        }

    class FunctionalInterface(val irClass: IrClass, val descriptor: FunctionClassDescriptor, val arity: Int)

    fun buildAllClasses() {
        val maxArity = 255 // See [BuiltInFictitiousFunctionClassFactory].
        (0..maxArity).forEach { arity ->
            functionN(arity)
            kFunctionN(arity)
            suspendFunctionN(arity)
            kSuspendFunctionN(arity)
        }
    }

    override fun functionClassDescriptor(arity: Int): FunctionClassDescriptor =
            irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor

    override fun kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            reflectionTypes.getKFunction(arity) as FunctionClassDescriptor

    override fun suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor

    override fun kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            reflectionTypes.getKSuspendFunction(arity) as FunctionClassDescriptor

    override fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor, declarator)

    override fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(reflectionTypes.getKFunction(arity) as FunctionClassDescriptor, declarator)

    override fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor, declarator)

    override fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(reflectionTypes.getKSuspendFunction(arity) as FunctionClassDescriptor, declarator)

    private val functionSymbol = symbolTable.descriptorExtension.referenceClass(irBuiltIns.builtIns.builtInsModule.findClassAcrossModuleDependencies(
            ClassId.topLevel(KonanFqNames.function))!!)

    private val kFunctionSymbol = symbolTable.descriptorExtension.referenceClass(irBuiltIns.builtIns.builtInsModule.findClassAcrossModuleDependencies(
            ClassId.topLevel(KonanFqNames.kFunction))!!)

    private val filesMap = mutableMapOf<PackageFragmentDescriptor, IrFile>()

    private val builtClassesMap = mutableMapOf<FunctionClassDescriptor, IrClass>()

    val builtFunctionNClasses get() = builtClassesMap.entries.mapNotNull { (descriptor, irClass) ->
        with(descriptor) {
            if (functionTypeKind == FunctionTypeKind.Function)
                FunctionalInterface(irClass, descriptor, arity)
            else null
        }
    }

    private fun createTypeParameter(descriptor: TypeParameterDescriptor): IrTypeParameter =
            symbolTable.descriptorExtension.declareGlobalTypeParameter(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, DECLARATION_ORIGIN_FUNCTION_CLASS,
                    descriptor
            )

    private fun createSimpleFunction(
        descriptor: FunctionDescriptor,
        origin: IrDeclarationOrigin,
        returnType: IrType,
        isFakeOverride: Boolean
    ): IrSimpleFunction {
        val functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction = {
            with(descriptor) {
                symbolTable.irFactory.createSimpleFunction(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        origin,
                        name,
                        visibility,
                        isInline,
                        isExpect,
                        returnType,
                        modality,
                        it,
                        isTailrec,
                        isSuspend,
                        isOperator,
                        isInfix,
                        isExternal,
                        isFakeOverride = isFakeOverride,
                )
            }
        }
        return symbolTable.descriptorExtension.declareSimpleFunction(descriptor, functionFactory)
    }

    private fun createIrClass(symbol: IrClassSymbol, descriptor: ClassDescriptor): IrClass =
        IrFactoryImpl.createIrClassFromDescriptor(offset, offset, DECLARATION_ORIGIN_FUNCTION_CLASS, symbol, descriptor)

    private fun createClass(descriptor: FunctionClassDescriptor, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
        symbolTable.declarator { createIrClass(it, descriptor) }

    private fun buildClass(descriptor: FunctionClassDescriptor, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            builtClassesMap.getOrPut(descriptor) {
                createClass(descriptor, declarator).apply {
                    val functionClass = this
                    typeParameters += descriptor.declaredTypeParameters.map { typeParameterDescriptor ->
                        createTypeParameter(typeParameterDescriptor).also {
                            it.parent = this
                            it.superTypes += irBuiltIns.anyNType
                        }
                    }

                    val descriptorToIrParametersMap = typeParameters.map { it.descriptor to it }.toMap()
                    superTypes += descriptor.typeConstructor.supertypes.map { superType ->
                        val arguments = superType.arguments.map { argument ->
                            val argumentClassifierDescriptor = argument.type.constructor.declarationDescriptor
                            val argumentClassifierSymbol = argumentClassifierDescriptor?.let { descriptorToIrParametersMap[it] }
                                    ?: error("Unexpected super type argument: $argumentClassifierDescriptor")
                            makeTypeProjection(argumentClassifierSymbol.defaultType, argument.projectionKind)
                        }
                        val superTypeSymbol = when (val superTypeDescriptor = superType.constructor.declarationDescriptor) {
                            is FunctionClassDescriptor -> buildClass(superTypeDescriptor) {
                                descriptorExtension.declareClass(superTypeDescriptor) {
                                    createIrClass(it, superTypeDescriptor)
                                }
                            }.symbol
                            functionSymbol.descriptor -> functionSymbol
                            kFunctionSymbol.descriptor -> kFunctionSymbol
                            else -> error("Unexpected super type: $superTypeDescriptor")
                        }
                        IrSimpleTypeImpl(superTypeSymbol, superType.isMarkedNullable, arguments, emptyList())
                    }

                    createThisReceiverParameter()

                    val invokeFunctionDescriptor = descriptor.unsubstitutedMemberScope.getContributedFunctions(
                            OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND).single()
                    val isFakeOverride = invokeFunctionDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                    if (!isFakeOverride) {
                        declarations += createSimpleFunction(
                                invokeFunctionDescriptor, DECLARATION_ORIGIN_FUNCTION_CLASS,
                                typeParameters.last().defaultType,
                                isFakeOverride
                        ).apply {
                            parent = functionClass
                            if (!isFakeOverride)
                                parameters += createDispatchReceiverParameterWithClassParent(DECLARATION_ORIGIN_FUNCTION_CLASS)
                            else {
                                val overriddenFunction = superTypes
                                        .mapNotNull { it.classOrNull?.owner }
                                        .single { it.descriptor is FunctionClassDescriptor }
                                        .simpleFunctions()
                                        .single { it.name == OperatorNameConventions.INVOKE }
                                overriddenSymbols += overriddenFunction.symbol
                                val dispatchReceiver = overriddenFunction.dispatchReceiverParameter?.copyTo(this)
                                if (dispatchReceiver != null) parameters += dispatchReceiver
                            }

                            parameters += invokeFunctionDescriptor.valueParameters.map {
                                symbolTable.irFactory.createValueParameter(
                                        startOffset = SYNTHETIC_OFFSET,
                                        endOffset = SYNTHETIC_OFFSET,
                                        origin = DECLARATION_ORIGIN_FUNCTION_CLASS,
                                        name = it.name,
                                        kind = IrParameterKind.Regular,
                                        type = functionClass.typeParameters[it.index].defaultType,
                                        isAssignable = false,
                                        symbol = IrValueParameterSymbolImpl(it),
                                        varargElementType = null,
                                        isCrossinline = it.isCrossinline,
                                        isNoinline = it.isNoinline,
                                        isHidden = false,
                                ).also { it.parent = this }
                            }
                        }
                    }
                    // Unfortunately, addFakeOverrides() uses some parents but they are only set after PsiToIr phase.
                    // So we add all the fake overrides only when we're supplied with the module (this is done after PsiToIr).
//                    if (this@BuiltInFictitiousFunctionIrClassFactory.module != null)
                        addFakeOverrides()

                    val packageFragmentDescriptor = descriptor.findPackage()
                    val file = filesMap.getOrPut(packageFragmentDescriptor) {
                        IrFileImpl(NaiveSourceBasedFileEntryImpl("[K][Suspend]Functions"), packageFragmentDescriptor).also {
                            this@BuiltInFictitiousFunctionIrClassFactory.module?.addFile(it)
                        }
                    }
                    parent = file
                    file.declarations += this
                }
            }

    private fun toIrType(wrapped: KotlinType): IrType {
        val kotlinType = wrapped.unwrap()
        return with(IrSimpleTypeBuilder()) {
            classifier = symbolTable.referenceClassifier(kotlinType.constructor.declarationDescriptor ?: error("No classifier for type $kotlinType"))
            nullability = SimpleTypeNullability.fromHasQuestionMark(kotlinType.isMarkedNullable)
            arguments = kotlinType.arguments.map {
                if (it.isStarProjection) IrStarProjectionImpl
                else makeTypeProjection(toIrType(it.type), it.projectionKind)
            }
            buildSimpleType()
        }
    }

    private fun IrFunction.createValueParameter(descriptor: ParameterDescriptor, kind: IrParameterKind): IrValueParameter {
        val varargType = if (descriptor is ValueParameterDescriptor) descriptor.varargElementType else null
        descriptor.dispatchReceiverParameter
        return symbolTable.irFactory.createValueParameter(
                startOffset = offset,
                endOffset = offset,
                origin = memberOrigin,
                kind = kind,
                name = descriptor.name,
                type = toIrType(descriptor.type),
                isAssignable = false,
                symbol = IrValueParameterSymbolImpl(descriptor),
                varargElementType = varargType?.let { toIrType(it) },
                isCrossinline = descriptor.isCrossinline,
                isNoinline = descriptor.isNoinline,
                isHidden = false,
        ).also {
            it.parent = this
        }
    }

    private fun IrClass.addFakeOverrides() {

        val fakeOverrideDescriptors = descriptor.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                .filterIsInstance<CallableMemberDescriptor>().filter { it.kind === CallableMemberDescriptor.Kind.FAKE_OVERRIDE }

        fun createFakeOverrideFunction(descriptor: FunctionDescriptor, property: IrPropertySymbol?): IrSimpleFunction {
            val returnType = descriptor.returnType?.let { toIrType(it) } ?: error("No return type for $descriptor")


            val functionDeclare = { s: IrSimpleFunctionSymbol ->
                descriptor.run {
                    symbolTable.irFactory.createSimpleFunction(
                            offset,
                            offset,
                            memberOrigin,
                            name,
                            visibility,
                            isInline,
                            isExpect,
                            returnType,
                            modality,
                            s,
                            isTailrec,
                            isSuspend,
                            isOperator,
                            isInfix,
                            isExternal,
                            isFakeOverride = true,
                    )
                }
            }

            val newFunction = symbolTable.descriptorExtension.declareSimpleFunction(descriptor, functionDeclare)

            newFunction.parent = this
            newFunction.overriddenSymbols = descriptor.overriddenDescriptors.mapNotNull { symbolTable.descriptorExtension.referenceSimpleFunction(it.original) }

            val descriptorParameters =
                    listOfNotNull(descriptor.dispatchReceiverParameter).associateWith { IrParameterKind.DispatchReceiver } +
                            descriptor.contextReceiverParameters.associateWith { IrParameterKind.Context } +
                            listOfNotNull(descriptor.extensionReceiverParameter).associateWith { IrParameterKind.ExtensionReceiver } +
                            descriptor.valueParameters.associateWith { IrParameterKind.Regular }
            newFunction.parameters += descriptorParameters.map { (param, kind) -> newFunction.createValueParameter(param, kind) }

            newFunction.correspondingPropertySymbol = property

            return newFunction
        }

        fun createFakeOverrideProperty(descriptor: PropertyDescriptor): IrProperty {
            val propertyDeclare = { s: IrPropertySymbol ->
                symbolTable.irFactory.createProperty(
                        startOffset = offset,
                        endOffset = offset,
                        origin = memberOrigin,
                        name = descriptor.name,
                        visibility = descriptor.visibility,
                        modality = descriptor.modality,
                        symbol = s,
                        isVar = descriptor.isVar,
                        isConst = descriptor.isConst,
                        isLateinit = descriptor.isLateInit,
                        isDelegated = descriptor.isDelegated,
                        isExternal = descriptor.isExternal,
                        isExpect = descriptor.isExpect,
                        isFakeOverride = true,
                )
            }
            val property = symbolTable.descriptorExtension.declareProperty(descriptor, propertyFactory = propertyDeclare)

            property.parent = this
            property.getter = descriptor.getter?.let { g -> createFakeOverrideFunction(g, property.symbol) }
            property.setter = descriptor.setter?.let { s -> createFakeOverrideFunction(s, property.symbol) }

            return property
        }


        fun createFakeOverride(descriptor: CallableMemberDescriptor): IrDeclaration {
            return when (descriptor) {
                is FunctionDescriptor -> createFakeOverrideFunction(descriptor, null)
                is PropertyDescriptor -> createFakeOverrideProperty(descriptor)
                else -> error("Unexpected member $descriptor")
            }
        }

        declarations += fakeOverrideDescriptors.map { createFakeOverride(it) }
    }
}

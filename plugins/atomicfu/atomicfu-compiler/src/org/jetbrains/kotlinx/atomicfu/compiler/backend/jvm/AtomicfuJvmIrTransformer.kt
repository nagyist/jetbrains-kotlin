/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer

class AtomicfuJvmIrTransformer(
    override val atomicfuSymbols: JvmAtomicSymbols,
    pluginContext: IrPluginContext
) : AbstractAtomicfuTransformer(pluginContext) {

    override val atomicfuPropertyTransformer: AtomicPropertiesTransformer = JvmAtomicPropertiesTransformer()
    override val atomicfuExtensionsTransformer: AtomicExtensionTransformer = JvmAtomicExtensionsTransformer()
    override val atomicfuFunctionCallTransformer: AtomicFunctionCallTransformer = JvmAtomicFunctionCallTransformer()

    private inner class JvmAtomicExtensionsTransformer : AtomicExtensionTransformer() {
        override fun transformedExtensionsForAllAtomicHandlers(atomicExtension: IrFunction): List<IrSimpleFunction> = listOf(
            generateExtensionForAtomicHandler(AtomicHandlerType.ATOMIC_FIELD_UPDATER, atomicExtension),
            generateExtensionForAtomicHandler(AtomicHandlerType.BOXED_ATOMIC, atomicExtension),
            generateExtensionForAtomicHandler(AtomicHandlerType.ATOMIC_ARRAY, atomicExtension)
        )
    }

    private inner class JvmAtomicPropertiesTransformer : AtomicPropertiesTransformer() {
        override fun IrProperty.delegateToTransformedProperty(originalDelegate: IrProperty) {
            val volatileProperty = atomicfuPropertyToVolatile[originalDelegate]
            // On JVM there are 2 options:
            // 1.  A given property is delegated to an in-class atomic ->
            //  a corresponding volatile property (with atomic field updaters) should already be registered -> delegate to this volatile property.
            // 2. A given property is delegated to a top-level atomic ->
            //  a corresponding [BoxedAtomic] handler should already be registered -> delegate to its accessors.
            if (volatileProperty != null) {
                delegateToVolatilePropertyAccessors(volatileProperty)
            } else {
                val atomicHandler = atomicfuPropertyToAtomicHandler[originalDelegate]
                require(atomicHandler != null && atomicHandler is BoxedAtomic) {
                    "A property ${originalDelegate.atomicfuRender()} was delegated to ${originalDelegate.atomicfuRender()} atomicfu property, " +
                            "but neither a its corresponding volatile property nor a Java boxed atomic handler was found."
                }
                getter?.delegateToBoxedAtomicAccessors(atomicHandler.declaration)
                setter?.delegateToBoxedAtomicAccessors(atomicHandler.declaration)
            }
        }

        private fun IrSimpleFunction.delegateToBoxedAtomicAccessors(boxedAtomic: IrProperty) {
            val accessor = this
            with(atomicfuSymbols.createBuilder(symbol)) {
                val dispatchReceiver = dispatchReceiverParameter?.capture()
                val getBoxedAtomicProperty = irGetProperty(boxedAtomic, dispatchReceiver)
                body = irBlockBody {
                    +irReturn(
                        if (accessor.isGetter) {
                            invokeFunctionOnAtomicHandler(AtomicHandlerType.BOXED_ATOMIC, getBoxedAtomicProperty, "get", emptyList(), accessor.returnType)
                        } else {
                            val arg = accessor.parameters.last().capture()
                            invokeFunctionOnAtomicHandler(AtomicHandlerType.BOXED_ATOMIC, getBoxedAtomicProperty, "set", listOf(arg), accessor.returnType)
                        }
                    )
                }
            }
        }
    }

    private inner class JvmAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun AtomicHandler<*>.getAtomicHandlerExtraArg(
            dispatchReceiver: IrExpression?,
            propertyGetterCall: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression? = when(this) {
            // OBJ: get class instance
            is AtomicFieldUpdater -> dispatchReceiver
            is AtomicFieldUpdaterValueParameter -> {
                require(parentFunction != null && parentFunction.isTransformedAtomicExtension())
                val obj = parentFunction.parameters.find { it.name.asString() == OBJ }!!.capture()
                obj
            }
            is AtomicArray -> getAtomicArrayElementIndex(propertyGetterCall)
            is AtomicArrayValueParameter -> getAtomicArrayElementIndex(parentFunction)
            else -> null
        }

        private fun AbstractAtomicfuIrBuilder.getAtomicHandlerReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?
        ): IrExpression = when(atomicHandler) {
            is AtomicFieldUpdater -> irGetProperty(atomicHandler.declaration, null)
            is BoxedAtomic, is AtomicArray -> irGetProperty(atomicHandler.declaration, dispatchReceiver)
            is AtomicFieldUpdaterValueParameter, is BoxedAtomicValueParameter, is AtomicArrayValueParameter -> atomicHandler.declaration.capture()
            else -> error("Unexpected atomic handler type for JVM backend: ${atomicHandler.javaClass.simpleName}")
        }

        override fun AbstractAtomicfuIrBuilder.getAtomicHandlerCallReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?
        ): IrExpression = getAtomicHandlerReceiver(atomicHandler, dispatchReceiver)

        override fun AbstractAtomicfuIrBuilder.getAtomicHandlerReceiver(
            atomicHandler: AtomicHandler<*>,
            dispatchReceiver: IrExpression?,
            parentFunction: IrFunction
        ): IrExpression = getAtomicHandlerReceiver(atomicHandler, dispatchReceiver)

        override fun valueParameterToAtomicHandler(valueParameter: IrValueParameter): AtomicHandler<*> =
            when {
                atomicfuSymbols.isBoxedAtomicHandlerType(valueParameter.type) -> BoxedAtomicValueParameter(valueParameter)
                atomicfuSymbols.isAtomicFieldUpdaterHandlerType(valueParameter.type) -> AtomicFieldUpdaterValueParameter(valueParameter)
                atomicfuSymbols.isAtomicArrayHandlerType(valueParameter.type) -> AtomicArrayValueParameter(valueParameter)
                else -> error("The type of the given valueParameter=${valueParameter.render()} does not match any of the JVM AtomicHandler types.")
            }
    }

    override fun IrFunction.checkAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType): Boolean =
        when (atomicHandlerType) {
            AtomicHandlerType.ATOMIC_FIELD_UPDATER -> {
                parameters.size > 3 &&
                        holdsAt(1, ATOMIC_HANDLER, atomicfuSymbols.javaFUClassSymbol(valueType).defaultType) &&
                        holdsAt(2, OBJ, irBuiltIns.anyNType)
            }
            AtomicHandlerType.BOXED_ATOMIC -> {
                parameters.size > 2 &&
                        holdsAt(1, ATOMIC_HANDLER, atomicfuSymbols.javaAtomicBoxClassSymbol(valueType).defaultType)
            }
            AtomicHandlerType.ATOMIC_ARRAY -> {
                val arrayClassSymbol = atomicfuSymbols.getAtomicArrayClassByValueType(valueType)
                val type = if (arrayClassSymbol.owner.typeParameters.isNotEmpty()) {
                    arrayClassSymbol.typeWith(valueType)
                } else {
                    arrayClassSymbol.defaultType
                }
                parameters.size > 3 &&
                        holdsAt(1, ATOMIC_HANDLER, type) &&
                        holdsAt(2, INDEX, irBuiltIns.intType)
            }
            else -> error("Unexpected atomic handler type for JVM backend: $atomicHandlerType")
        }

    override fun IrFunction.addAtomicHandlerValueParameters(atomicHandlerType: AtomicHandlerType, valueType: IrType) {
        when (atomicHandlerType) {
            AtomicHandlerType.ATOMIC_FIELD_UPDATER -> {
                addValueParameter(ATOMIC_HANDLER, atomicfuSymbols.javaFUClassSymbol(valueType).defaultType)
                addValueParameter(OBJ, irBuiltIns.anyNType)
            }
            AtomicHandlerType.BOXED_ATOMIC -> {
                addValueParameter(ATOMIC_HANDLER, atomicfuSymbols.javaAtomicBoxClassSymbol(valueType).defaultType)
            }
            AtomicHandlerType.ATOMIC_ARRAY -> {
                val arrayClassSymbol = atomicfuSymbols.getAtomicArrayClassByValueType(valueType)
                val type = if (arrayClassSymbol.owner.typeParameters.isNotEmpty()) {
                    arrayClassSymbol.typeWith(valueType)
                } else {
                    arrayClassSymbol.defaultType
                }
                addValueParameter(ATOMIC_HANDLER, type)
                addValueParameter(INDEX, irBuiltIns.intType)
            }
            else -> error("Unexpected atomic handler type for JVM backend: $atomicHandlerType")
        }
    }

    override fun createAtomicHandler(
        atomicfuProperty: IrProperty,
        parentContainer: IrDeclarationContainer
    ): AtomicHandler<IrProperty>? {
        val isTopLevel = parentContainer is IrFile || (parentContainer is IrClass && parentContainer.kind == ClassKind.OBJECT)
        with(atomicfuSymbols.createBuilder(atomicfuProperty.symbol)) {
            return when {
                atomicfuProperty.isNotDelegatedAtomic() -> {
                    if (isTopLevel) {
                        buildBoxedAtomic(atomicfuProperty, parentContainer)
                    } else {
                        buildAtomicFieldUpdater(atomicfuProperty, parentContainer as IrClass)
                    }
                }
                atomicfuProperty.isAtomicArray() -> {
                    createAtomicArray(atomicfuProperty, parentContainer)
                }
                else -> null
            }
        }
    }
}
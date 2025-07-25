/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeNames
import androidx.compose.compiler.plugins.kotlin.hasComposableAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer
import java.util.*

fun IrElement.dumpSrc(useFir: Boolean = false): String {
    val sb = StringBuilder()
    accept(IrSourcePrinterVisitor(sb, "%tab%", useFir), null)
    return sb
        .toString()
        // replace tabs at beginning of line with white space
        .replace(Regex("\\n(%tab%)+", RegexOption.MULTILINE)) {
            val size = it.range.last - it.range.first - 1
            "\n" + (0..(size / 5)).joinToString("") { "  " }
        }
        // tabs that are inserted in the middle of lines should be replaced with empty strings
        .replace(Regex("%tab%", RegexOption.MULTILINE), "")
        // remove empty lines
        .replace(Regex("\\n(\\s)*$", RegexOption.MULTILINE), "")
        // brackets with comma on new line
        .replace(Regex("}\\n(\\s)*,", RegexOption.MULTILINE), "},")
}

class Scope(
    val owner: IrFunction? = null,
    val localValues: HashSet<IrValueDeclaration> = hashSetOf(),
)

class IrSourcePrinterVisitor(
    out: Appendable,
    indentUnit: String = "  ",
    private val useFir: Boolean = false,
) : IrVisitorVoid() {
    private val printer = Printer(out, indentUnit)
    private var currentScope: Scope = Scope()

    private fun IrElement.print() {
        accept(this@IrSourcePrinterVisitor, null)
    }

    private fun print(obj: Any?) = printer.print(obj)
    private fun println(obj: Any?) = printer.println(obj)
    private fun println() = printer.println()

    fun printType(type: IrType) = type.renderSrc()

    private inline fun IrFunction.scoped(block: (IrFunction) -> Unit) {
        val previousScope = currentScope
        currentScope = Scope(
            this,
            HashSet(parameters)
        )

        block(this)

        currentScope = previousScope
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    private inline fun bracedBlock(body: () -> Unit) {
        println("{")
        indented(body)
        println()
        println("}")
    }

    private fun List<IrElement>.printJoin(separator: String = "") {
        forEachIndexed { index, it ->
            it.print()
            if (index < size - 1) print(separator)
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
//        println("// MODULE: ${declaration.name}")
        declaration.files.printJoin()
    }

    override fun visitFile(declaration: IrFile) {
        includeFileNameInExceptionTrace(declaration) {
//          println("// FILE: ${declaration.fileEntry.name}")
            declaration.declarations.printJoin("\n")
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        if (declaration.isCrossinline) {
            print("crossinline ")
        }
        if (declaration.isNoinline) {
            print("noinline ")
        }
        declaration.printAnnotations()
        print(declaration.normalizedName)
        print(": ")
        print(declaration.type.renderSrc())
        declaration.defaultValue?.let { it ->
            print(" = ")
            it.print()
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return
        declaration.scoped {
            declaration.printAnnotations(onePerLine = true)
            if (declaration.parameters.any { it.kind == IrParameterKind.Context }) {
                print("context(")
                declaration.parameters.filter { it.kind == IrParameterKind.Context }.printJoin(", ")
                println(")")
            }
            if (declaration.overriddenSymbols.isNotEmpty()) {
                print("override ")
            } else {
                if (
                    declaration.visibility != DescriptorVisibilities.PUBLIC &&
                    declaration.visibility != DescriptorVisibilities.LOCAL
                ) {
                    print(declaration.visibility.toString().lowercase(Locale.ROOT))
                    print(" ")
                }
                if (declaration.modality != Modality.FINAL) {
                    print(declaration.modality.toString().lowercase(Locale.ROOT))
                    print(" ")
                }
            }
            if (declaration.isSuspend) {
                print("suspend ")
            }
            print("fun ")
            if (declaration.typeParameters.isNotEmpty()) {
                print("<")
                declaration.typeParameters.printJoin(", ")
                print("> ")
            }
            declaration.firstParameterOfKind(IrParameterKind.ExtensionReceiver)?.let {
                print(it.type.renderSrc())
                print(".")
            }
            print(declaration.name)
            print("(")
            declaration.parameters.filter { it.kind == IrParameterKind.Regular }.printJoin(", ")
            print(")")
            if (!declaration.returnType.isUnit()) {
                print(": ")
                print(
                    declaration.returnType.renderSrc()
                )
            }
            print(" ")
            declaration.printBody()
        }
    }

    private val IrBody.statements: List<IrStatement>
        get() = when (this) {
            is IrBlockBody -> statements
            is IrExpressionBody -> listOf(expression)
            is IrSyntheticBody -> emptyList()
        }


    fun IrFunction.printBody() {
        val body = body ?: return
        if (body.statements.isEmpty()) {
            println("{ }")
        } else {
            bracedBlock {
                body.print()
            }
        }
    }

    override fun visitConstructor(declaration: IrConstructor) {
        declaration.printAnnotations(onePerLine = true)
        print("constructor")
        val parameters = declaration.parameters
        if (parameters.isNotEmpty()) {
            print("(")
            parameters.printJoin(", ")
            print(")")
        }
        declaration.printBody()
    }

    private var isInNotCall = false

    private fun IrFunctionAccessExpression.printReceiver() {
        arguments[symbol.owner.parameters.first { it.isReceiver }.indexInParameters]?.print()
    }

    private fun IrFunctionAccessExpression.printFirstParameter() {
        arguments[symbol.owner.firstParameterOfKind(IrParameterKind.Regular)!!.indexInParameters]?.print()
    }

    private fun IrFunctionAccessExpression.argumentForKind(
        kind: IrParameterKind,
    ): IrExpression? {
        return symbol.owner.firstParameterOfKind(kind)?.indexInParameters?.let { arguments[it] }
    }

    override fun visitCall(expression: IrCall) {
        val function = expression.symbol.owner
        val name = function.name.asString()
        val isOperator = function.isOperator || function.origin == IrBuiltIns.BUILTIN_OPERATOR
        val isInfix = function.isInfix
        if (isOperator) {
            if (name == "not") {
                // IR tree for `a != b` looks like `not(equals(a, b))` which makes
                // it challenging to print it like the former. To do so, we capture when we are in
                // a "not" call, and then check to see if the argument is an equals call. if it is,
                // we will just print the child call and put the transformer into a mode where it
                // knows to print the negative
                val arg = expression.dispatchReceiver!!
                if (arg is IrCall) {
                    val fn = arg.symbol.owner
                    if (fn.origin == IrBuiltIns.BUILTIN_OPERATOR) {
                        when (fn.name.asString()) {
                            "equals",
                            "EQEQ",
                            "EQEQEQ" -> {
                                val prevIsInNotCall = isInNotCall
                                isInNotCall = true
                                arg.print()
                                isInNotCall = prevIsInNotCall
                                return
                            }
                        }
                    }
                }
            }
            val opSymbol = when (name) {
                "contains" -> "in"
                "equals" -> if (isInNotCall) "!=" else "=="
                "plus" -> "+"
                "not" -> "!"
                "minus" -> "-"
                "times" -> "*"
                "div" -> "/"
                "rem" -> "%"
                "rangeTo" -> ".."
                "plusAssign" -> "+="
                "minusAssign" -> "-="
                "unaryMinus" -> "-"
                "timesAssign" -> "*="
                "divAssign" -> "/="
                "remAssign" -> "%="
                "inc" -> "++"
                "dec" -> "--"
                "greater" -> ">"
                "less" -> "<"
                "lessOrEqual" -> "<="
                "greaterOrEqual" -> ">="
                "EQEQ" -> if (isInNotCall) "!=" else "=="
                "EQEQEQ" -> if (isInNotCall) "!==" else "==="
                "OROR" -> "||"
                "ieee754equals" -> "=="

                // no names for
                "invoke", "get", "set" -> ""
                "iterator", "hasNext", "next", "getValue", "setValue",
                "noWhenBranchMatchedException" -> name
                "CHECK_NOT_NULL" -> "!!"
                "THROW_ISE" -> "throw IllegalStateException()"
                else -> {
                    if (name.startsWith("component")) name
                    else error("Unhandled operator $name")
                }
            }

            val printBinary = when (name) {
                "equals",
                "EQEQ",
                "EQEQEQ" -> {
                    expression.argumentForKind(IrParameterKind.DispatchReceiver)?.type?.isInt() == true ||
                            expression.argumentForKind(IrParameterKind.ExtensionReceiver)?.type?.isInt() == true ||
                            function.namedParameters.let {
                                it.isNotEmpty() &&
                                        expression.arguments[it.first().indexInParameters]?.type?.isInt() == true
                            }
                }
                else -> false
            }
            val prevPrintBinary = printIntsAsBinary
            printIntsAsBinary = printBinary
            when (name) {
                // unary prefx
                "unaryPlus", "unaryMinus", "not" -> {
                    print(opSymbol)
                    expression.printReceiver()
                }
                // unary postfix
                "inc", "dec" -> {
                    expression.printReceiver()
                    print(opSymbol)
                }
                "CHECK_NOT_NULL" -> {
                    expression.arguments[0]?.print()
                    print(opSymbol)
                }
                "getValue", "setValue" -> {
                    expression.printReceiver()
                    print(".")
                    print(opSymbol)
                    expression.printArgumentList()
                }
                "invoke" -> {
                    expression.printReceiver()
                    expression.printArgumentList()
                }
                // get indexer
                "get" -> {
                    expression.printReceiver()
                    print("[")
                    for (i in 0 until expression.arguments.size) {
                        if (function.parameters[i].kind == IrParameterKind.Regular) {
                            val arg = expression.arguments[i]
                            arg?.print()
                        }

                    }
                    print("]")
                }
                // set indexer
                "set" -> {
                    expression.printReceiver()
                    print("[")
                    expression.arguments[function.parameters.first { it.kind == IrParameterKind.Regular }.indexInParameters]?.print()
                    print("] = ")
                    expression.arguments[function.parameters.last { it.kind == IrParameterKind.Regular }.indexInParameters]?.print()
                }
                // builtin static operators
                "greater", "less", "lessOrEqual", "greaterOrEqual", "EQEQ", "EQEQEQ",
                "ieee754equals" -> {
                    expression.arguments[0]?.print()
                    print(" $opSymbol ")
                    expression.arguments[1]?.print()
                }
                "iterator", "hasNext", "next" -> {
                    expression.printReceiver()
                    print(".")
                    print(opSymbol)
                    print("()")
                }
                "THROW_ISE", "noWhenBranchMatchedException" -> {
                    print(opSymbol)
                    print("()")
                }
                else -> {
                    if (name.startsWith("component")) {
                        expression.printReceiver()
                        print(".")
                        print(opSymbol)
                        print("()")
                    } else {
                        // else binary
                        expression.printReceiver()
                        print(" $opSymbol ")
                        expression.printFirstParameter()
                    }
                }
            }
            printIntsAsBinary = prevPrintBinary
            return
        }

        if (isInfix) {
            val prev = printIntsAsBinary
            if (name == "xor" || name == "and" || name == "or") {
                printIntsAsBinary = true
            }
            expression.printReceiver()
            print(" $name ")
            expression.printFirstParameter()
            printIntsAsBinary = prev
            return
        }

        expression.printExplicitReceiver(".", expression.superQualifierSymbol)

        val prop = function.correspondingPropertySymbol?.owner

        if (prop != null && !function.hasComposableAnnotation()) {
            val propName = prop.name.asString()
            print(propName)
            if (function == prop.setter) {
                print(" = ")
                expression.printFirstParameter()
            }
        } else {
            print(name)
            expression.printArgumentList()
        }
    }

    private fun IrAnnotationContainer.printAnnotations(onePerLine: Boolean = false) {
        if (annotations.isNotEmpty()) {
            annotations.printJoin(if (onePerLine) "\n" else " ")
            if (onePerLine) println()
            else print(" ")
        }
    }

    private fun IrMemberAccessExpression<*>.printExplicitReceiver(
        suffix: String? = null,
        superQualifierSymbol: IrClassSymbol? = null,
    ) {
        val dispatchReceiverArg: IrExpression?
        val extensionReceiverArg: IrExpression?

        when (this) {
            is IrFunctionAccessExpression, is IrFunctionReference -> {
                val owner = symbol.owner
                val dispatchReceiver = owner.firstParameterOfKind(IrParameterKind.DispatchReceiver)
                val extensionReceiver = owner.firstParameterOfKind(IrParameterKind.ExtensionReceiver)
                dispatchReceiverArg = dispatchReceiver?.let { arguments[it.indexInParameters] }
                extensionReceiverArg = extensionReceiver?.let { arguments[it.indexInParameters] }
            }
            is IrPropertyReference -> {
                dispatchReceiverArg = arguments.firstOrNull()
                extensionReceiverArg = null
            }
            else -> {
                error("Unknown expression type: ${dump()}")
            }
        }

        val dispatchIsSpecial = dispatchReceiverArg.let {
            it is IrGetValue && it.symbol.owner.name.isSpecial
        }
        val extensionIsSpecial = extensionReceiverArg.let {
            it is IrGetValue && it.symbol.owner.name.isSpecial
        }

        if (superQualifierSymbol != null) {
            print("super<${superQualifierSymbol.owner.name}>")
            suffix?.let(::print)
        } else if (dispatchReceiverArg != null && !dispatchIsSpecial) {
            dispatchReceiverArg.print()
            suffix?.let(::print)
        } else if (extensionReceiverArg != null && !extensionIsSpecial) {
            extensionReceiverArg.print()
            suffix?.let(::print)
        }
    }

    private fun IrFunctionAccessExpression.printArgumentList(
        forceParameterNames: Boolean = false,
        forceSingleLine: Boolean = false,
    ) {
        val arguments = mutableListOf<IrExpression>()
        val paramNames = mutableListOf<String>()
        var trailingLambda: IrExpression? = null
        var useParameterNames = forceParameterNames
        for (i in this.arguments.indices) {
            val p = symbol.owner.parameters[i]
            val arg = this.arguments[i]
            if (p.isReceiver) continue

            if (arg != null) {
                val isLambda = arg is IrFunctionExpression ||
                        (arg is IrBlock &&
                                (arg.origin == IrStatementOrigin.LAMBDA))
                if (isLambda) {
                    arg.unwrapLambda()?.let {
                        returnTargetToCall[it] = this
                    }
                }
                val isTrailingLambda = i == symbol.owner.parameters.lastIndex && isLambda
                if (isTrailingLambda) {
                    trailingLambda = arg
                } else {
                    arguments.add(arg)
                    paramNames.add(p.normalizedName)
                }
            } else {
                useParameterNames = true
            }
        }
        val multiline = arguments.isNotEmpty() && useParameterNames && !forceSingleLine
        if (arguments.isNotEmpty() || trailingLambda == null) {
            print("(")
            if (multiline) {
                // if we are using parameter names, we go on multiple lines
                println()
                indented {
                    arguments.zip(paramNames).forEachIndexed { i, (arg, name) ->
                        print(name)
                        print(" = ")
                        arg.print()
                        if (i < arguments.size - 1) println(", ")
                    }
                }
                println()
            } else {
                arguments.zip(paramNames).forEachIndexed { i, (arg, name) ->
                    if (useParameterNames) {
                        print(name)
                        print(" = ")
                    }
                    when {
                        name.startsWith(ComposeNames.DefaultParameter.identifier) ||
                                name.startsWith(ComposeNames.ChangedParameter.identifier) -> {
                            withIntsAsBinaryLiterals {
                                arg.print()
                            }
                        }
                        else -> {
                            arg.printWithExplicitBlock()
                        }
                    }
                    if (i < arguments.size - 1) print(", ")
                }
            }
            print(")")
        }
        trailingLambda?.let {
            print(" ")
            it.print()
        }
    }

    fun IrElement.printWithExplicitBlock() {
        when (this) {
            is IrBlock -> {
                println("<block>{")
                indented { print() }
                println()
                print("}")
            }
            else -> print()
        }
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression) {
        expression.function.printAsLambda()
    }

    fun IrFunction.printAsLambda() {
        scoped {
            print("{")
            val parameters = namedParameters
            if (parameters.isNotEmpty()) {
                print(" ")
                parameters.printJoin(", ")
                println(" ->")
            } else {
                println()
            }
            indented {
                body?.print()
            }
            println()
            println("}")
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                expression.argument.print()
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                expression.argument.print()
                print(" !is ")
                print(expression.type.renderSrc())
            }
            IrTypeOperator.CAST, IrTypeOperator.SAFE_CAST, IrTypeOperator.IMPLICIT_CAST -> {
                expression.argument.print()
                print(" as ")
                print(expression.type.renderSrc())
            }
            IrTypeOperator.SAM_CONVERSION -> {
                print(expression.type.renderSrc())
                print(" ")
                expression.argument.print()
            }
            IrTypeOperator.IMPLICIT_NOTNULL -> {
                expression.argument.print()
                print("!!")
            }
            IrTypeOperator.INSTANCEOF -> {
                expression.argument.print()
                print(" is ")
                print(expression.typeOperand.renderSrc())
            }
            else -> error("Unknown type operator: ${expression.operator}")
        }
    }

    override fun visitComposite(expression: IrComposite) {
        expression.statements.printJoin("\n")
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        println("do {")
        indented {
            loop.body?.print()
            println()
        }
        print("} while (")
        loop.condition.print()
        println(")")
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        val constructedClass = expression.symbol.owner.constructedClass
        val name = constructedClass.name
        val isAnnotation = constructedClass.isAnnotationClass
        if (isAnnotation) {
            print("@")
        }
        expression.dispatchReceiver?.let {
            it.print()
            print(".")
        }
        print(name)

        val printArgumentList = if (isAnnotation) {
            expression.arguments.any { it != null }
        } else {
            true
        }
        if (printArgumentList) {
            expression.printArgumentList(
                forceParameterNames = isAnnotation,
                forceSingleLine = isAnnotation
            )
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        val arguments = expression.arguments
        print("\"")
        for (arg in arguments) {
            when {
                arg is IrConst && arg.kind == IrConstKind.String -> print(arg.value)
                arg is IrGetValue -> {
                    print("$")
                    arg.print()
                }
                else -> {
                    print("\${")
                    arg.print()
                    print("}")
                }
            }
        }
        print("\"")
    }

    override fun visitVararg(expression: IrVararg) {
        expression
            .elements
            .forEachIndexed { index, it ->
                if (it is IrSpreadElement) {
                    print("*")
                    it.expression.print()
                } else if (it is IrExpression) {
                    it.print()
                }
                if (index < expression.elements.size - 1) print(", ")
            }
    }

    override fun visitWhen(expression: IrWhen) {
        val isIf = expression.origin == IrStatementOrigin.IF
        when {
            expression.origin == IrStatementOrigin.OROR -> {
                val lhs = expression.branches[0].condition
                val rhs = expression.branches[1].result
                lhs.print()
                print(" || ")
                rhs.print()
            }
            expression.origin == IrStatementOrigin.ANDAND -> {
                val lhs = expression.branches[0].condition
                val rhs = expression.branches[0].result
                lhs.print()
                print(" && ")
                rhs.print()
            }
            isIf -> {
                val singleLine = expression.branches.all {
                    it.result is IrConst || it.result is IrGetValue
                }
                expression.branches.forEachIndexed { index, branch ->
                    val isElse = index == expression.branches.size - 1 &&
                            (branch.condition as? IrConst)?.value == true
                    when {
                        index == 0 -> {
                            print("if (")
                            branch.condition.print()
                            if (singleLine)
                                print(") ")
                            else
                                println(") {")
                        }
                        isElse -> {
                            if (singleLine)
                                print(" else ")
                            else
                                println("} else {")
                        }
                        else -> {
                            if (singleLine)
                                print(" else if (")
                            else
                                print("} else if (")
                            branch.condition.print()
                            if (singleLine)
                                print(") ")
                            else
                                println(") {")
                        }
                    }
                    if (singleLine)
                        branch.result.print()
                    else indented {
                        branch.result.print()
                        println()
                    }
                }
                if (!singleLine)
                    println("}")
            }
            else -> {
                print("when ")
                bracedBlock {
                    expression.branches.forEach {
                        val isElse = (it.condition as? IrConst)?.value == true

                        if (isElse) {
                            print("else")
                        } else {
                            it.condition.print()
                        }
                        print(" -> ")
                        bracedBlock {
                            it.result.print()
                        }
                    }
                }
            }
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop) {
        if (loop.label != null) {
            print(loop.label)
            print("@")
        }
        print("while (")
        loop.condition.print()
        println(") {")
        indented {
            loop.body?.print()
            println()
        }
        println("}")
    }

    // Map local return targets to the corresponding function call.
    // This is used to print qualified returns.
    private val returnTargetToCall =
        mutableMapOf<IrReturnTargetSymbol, IrFunctionAccessExpression>()

    private val IrFunction.isLambda: Boolean
        get() = name.asString() == SpecialNames.ANONYMOUS_STRING ||
                origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE

    private val IrFunction.isDelegatedPropertySetter: Boolean
        get() = isSetter && origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR

    private fun IrExpression.isLastStatementIn(statements: List<IrStatement>): Boolean {
        val lastStatement = statements.lastOrNull()
        return when {
            lastStatement === this -> true
            lastStatement is IrBlock -> isLastStatementIn(lastStatement.statements)
            else -> false
        }
    }

    private fun IrExpression.isLastStatementIn(function: IrFunction): Boolean =
        function.body?.let { isLastStatementIn(it.statements) } ?: false

    override fun visitReturn(expression: IrReturn) {
        val value = expression.value
        // Only print the return statement directly if it is not the last statement in a lambda,
        // or a delegated property setter. The latter have a superfluous "return" in K1.
        val returnTarget = expression.returnTargetSymbol.owner
        if (returnTarget !is IrFunction ||
            (!returnTarget.isLambda && (useFir || !returnTarget.isDelegatedPropertySetter)) ||
            !expression.isLastStatementIn(returnTarget)
        ) {
            val suffix = returnTargetToCall[returnTarget.symbol]?.let {
                "@${it.symbol.owner.name}"
            } ?: ""
            print("return$suffix ")
        }
        if (value.type.isUnit() && value is IrGetObjectValue) {
            return
        }
        value.print()
    }

    override fun visitBlock(expression: IrBlock) {
        when (expression.origin) {
            IrStatementOrigin.POSTFIX_INCR -> {
                val tmpVar = expression.statements[0] as IrVariable
                val lhs = tmpVar.initializer ?: error("Expected initializer")
                lhs.print()
                print("++")
            }
            IrStatementOrigin.POSTFIX_DECR -> {
                val tmpVar = expression.statements[0] as IrVariable
                val lhs = tmpVar.initializer ?: error("Expected initializer")
                lhs.print()
                print("--")
            }
            IrStatementOrigin.LAMBDA -> {
                val function = expression.statements[0] as IrFunction
                function.printAsLambda()
            }
            IrStatementOrigin.OBJECT_LITERAL -> {
                val classImpl = expression.statements[0] as IrClass
                classImpl.printAsObject()
            }
            IrStatementOrigin.SAFE_CALL -> {
                val lhs = expression.statements[0] as IrVariable
                val rhsStatement = expression.statements[1]
                val rhs = when (rhsStatement) {
                    is IrBlock -> {
                        if (rhsStatement.statements.size == 2) {
                            val target = rhsStatement.statements[1]
                            when (target) {
                                is IrVariable -> target.initializer
                                else -> target
                            }
                        } else rhsStatement
                    }
                    else -> {
                        rhsStatement
                    }
                } as? IrWhen
                val call = rhs?.let { it.branches.last().result as? IrCall }
                if (call == null) {
                    expression.statements.printJoin("\n")
                    return
                }
                lhs.initializer?.print()
                print("?.")
                print(call.symbol.owner.name)
                call.printArgumentList()
            }
            IrStatementOrigin.FOR_LOOP -> {
                expression.statements.printJoin("\n")
            }
            else -> {
                expression.statements.printJoin("\n")
            }
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        currentScope.localValues.add(declaration)

        if (declaration.isLateinit) {
            print("lateinit")
        }
        when {
            declaration.isConst -> print("const ")
            declaration.isVar -> print("var ")
            else -> print("val ")
        }
        print(declaration.normalizedName)
        declaration.initializer?.let {
            print(" = ")
            it.printWithExplicitBlock()
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        print(expression.symbol.owner.name)
    }

    override fun visitGetValue(expression: IrGetValue) {
        val owner = expression.symbol.owner
        print(owner.normalizedName)

        if (
            owner.parent != currentScope.owner &&
            currentScope.localValues.any { it.name == owner.name }
        ) {
            print("@")
            print(owner.parent.kotlinFqName)
        }
    }

    override fun visitField(declaration: IrField) {
        if (
            declaration.visibility != DescriptorVisibilities.PUBLIC &&
            declaration.visibility != DescriptorVisibilities.LOCAL
        ) {
            print(declaration.visibility.toString().lowercase(Locale.ROOT))
            print(" ")
        }
        if (declaration.isStatic) {
            print("static ")
        }
        if (declaration.isFinal) {
            print("val ")
        } else {
            print("var ")
        }
        print(declaration.symbol.owner.name)
        print(": ")
        val type = declaration.type
        print(type.renderSrc())
        declaration.initializer?.let {
            print(" = ")
            it.printWithExplicitBlock()
        }
    }

    override fun visitGetField(expression: IrGetField) {
        val receiver = expression.receiver
        val owner = expression.symbol.owner
        val parent = owner.parent
        if (receiver != null) {
            expression.receiver?.print()
        } else if (owner.isStatic && parent is IrClass) {
            print(parent.name)
        }
        print(".")
        print(owner.name)
    }

    override fun visitSetField(expression: IrSetField) {
        expression.receiver?.print()
        print(".")
        print(expression.symbol.owner.name)
        print(" = ")
        expression.value.printWithExplicitBlock()
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue) {
        val classifier = expression.type.classOrNull ?: return
        print(classifier.owner.name)
        print(".")
        print(expression.symbol.owner.name)
    }

    override fun visitSetValue(expression: IrSetValue) {
        print(expression.symbol.owner.normalizedName)
        print(" = ")
        expression.value.printWithExplicitBlock()
    }

    override fun visitExpressionBody(body: IrExpressionBody) {
        body.expression.accept(this, null)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.printAnnotations(onePerLine = true)
        if (declaration.isLateinit) {
            print("lateinit")
        }
        when {
            declaration.isConst -> print("const ")
            declaration.isVar -> print("var ")
            else -> print("val ")
        }
        print(declaration.name)
        print(": ")
        val type = declaration.backingField?.type
            ?: declaration.getter?.returnType
            ?: error("Couldn't find return type")
        print(type.renderSrc())
        declaration.backingField?.let { field ->
            field.initializer?.let { initializer ->
                print(" = ")
                initializer.print()
            }
        }
        indented {
            declaration.getter?.scoped {
                if (it.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
                    println()
                    it.printAnnotations()
                    println()
                    println("get() {")
                    indented {
                        it.body?.accept(this, null)
                    }
                    println()
                    println("}")
                }
            }
            declaration.setter?.scoped {
                if (it.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
                    println()
                    it.printAnnotations()
                    println("set(value) {")
                    indented {
                        it.body?.accept(this, null)
                    }
                    println()
                    println("}")
                }
            }
        }
    }

    private var printIntsAsBinary = false
    fun <T> withIntsAsBinaryLiterals(block: () -> T): T {
        val prev = printIntsAsBinary
        try {
            printIntsAsBinary = true
            return block()
        } finally {
            printIntsAsBinary = prev
        }
    }

    private fun intAsBinaryString(value: Int): String {
        if (value == 0) return "0"
        var current = if (value >= 0) value else value.inv()
        var result = ""
        while (current != 0 || result.length % 4 != 0) {
            val nextBit = current and 1 != 0
            current = current ushr 1
            result = "${if (nextBit) "1" else "0"}$result"
        }
        return "0b$result" + if (value < 0) ".inv()" else ""
    }

    override fun visitConst(expression: IrConst) {
        val result = when (expression.kind) {
            is IrConstKind.Null -> "${expression.value}"
            is IrConstKind.Boolean -> "${expression.value}"
            is IrConstKind.Char -> "'${expression.value}'"
            is IrConstKind.Byte -> "${expression.value}"
            is IrConstKind.Short -> "${expression.value}"
            is IrConstKind.Int -> {
                if (printIntsAsBinary) {
                    intAsBinaryString(expression.value as Int)
                } else {
                    "${expression.value}"
                }
            }
            is IrConstKind.Long -> "${expression.value}L"
            is IrConstKind.Float -> "${expression.value}f"
            is IrConstKind.Double -> "${expression.value}"
            is IrConstKind.String -> "\"${expression.value}\""
        }
        print(result)
    }

    override fun visitBlockBody(body: IrBlockBody) {
        body.statements.printJoin("\n")
    }

    private fun IrClass.correspondingProperty(param: IrValueParameter): IrProperty? {
        return declarations
            .mapNotNull { it as? IrProperty }
            .firstOrNull {
                if (it.name == param.name) {
                    val init = it.backingField?.initializer?.expression as? IrGetValue
                    init?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
                } else false
            }
    }

    override fun visitClass(declaration: IrClass) {
        val primaryConstructor = declaration.primaryConstructor
        declaration.printAnnotations(onePerLine = true)
        if (
            declaration.visibility != DescriptorVisibilities.PUBLIC &&
            declaration.visibility != DescriptorVisibilities.LOCAL
        ) {
            print(declaration.visibility.toString().lowercase(Locale.ROOT))
            print(" ")
        }
        if (declaration.isInner) {
            print("inner ")
        }
        if (declaration.isData) {
            print("data ")
        }
        if (declaration.isInterface) {
            print("interface ")
        } else if (declaration.isObject) {
            print("object ")
        } else {
            if (declaration.modality != Modality.FINAL) {
                print(declaration.modality.toString().lowercase(Locale.ROOT))
                print(" ")
            }
            if (declaration.isAnnotationClass) {
                print("annotation ")
            }
            print("class ")
        }
        print(declaration.name)
        if (declaration.typeParameters.isNotEmpty()) {
            print("<")
            declaration.typeParameters.printJoin(", ")
            print("> ")
        }
        val ctorProperties = mutableSetOf<IrProperty>()
        if (primaryConstructor != null) {
            val namedParameters = primaryConstructor.namedParameters
            if (namedParameters.isNotEmpty()) {
                print("(")
                namedParameters.forEachIndexed { index, param ->
                    val property = declaration.correspondingProperty(param)
                    if (property != null) {
                        ctorProperties.add(property)
                        print(if (property.isVar) "var " else "val ")
                    }
                    param.print()
                    if (index < namedParameters.size - 1) print(", ")
                }
                print(")")
            }
        }
        print(" ")
        if (declaration.superTypes.any { !it.isAny() }) {
            print(": ")
            print(declaration.superTypes.joinToString(", ") { it.renderSrc() })
            print(" ")
        }
        val nonParamDeclarations = declaration
            .declarations
            .filter { it != primaryConstructor && !ctorProperties.contains(it) }
            .filter { it.origin != IrDeclarationOrigin.FAKE_OVERRIDE }
        if (nonParamDeclarations.isNotEmpty()) {
            bracedBlock {
                nonParamDeclarations.printJoin("\n")
            }
        } else {
            println()
        }
    }

    fun IrClass.printAsObject() {
        print("object ")
        if (!name.isSpecial) {
            print(name)
            print(" ")
        }
        if (superTypes.any { !it.isAny() }) {
            print(": ")
            print(superTypes.joinToString(", ") { it.renderSrc() })
            print(" ")
        }
        val printableDeclarations = declarations
            .filter { it !is IrConstructor }
            .filter { it.origin != IrDeclarationOrigin.FAKE_OVERRIDE }
        if (printableDeclarations.isNotEmpty()) {
            bracedBlock {
                printableDeclarations.printJoin("\n")
            }
        } else {
            println()
        }
    }

    override fun visitBreak(jump: IrBreak) {
        print("break")
        if (jump.label != null) {
            print("@")
            print(jump.label)
        }
    }

    override fun visitContinue(jump: IrContinue) {
        print("continue")
        if (jump.label != null) {
            print("@")
            print(jump.label)
        }
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        print(declaration.name)
        val isNonEmpty = declaration.superTypes.isNotEmpty() &&
                !declaration.superTypes[0].isNullableAny()
        if (isNonEmpty) {
            print(": ")
            print(declaration.superTypes.joinToString(", ") { it.renderSrc() })
        }
    }

    override fun visitThrow(expression: IrThrow) {
        print("throw ")
        expression.value.print()
    }

    override fun visitClassReference(expression: IrClassReference) {
        print(expression.classType.renderSrc())
        print("::class")
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        super.visitFunctionAccess(expression)
    }

    override fun visitBranch(branch: IrBranch) {
        print("<<BRANCH>>")
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {
        print("<<BREAKCONTINUE>>")
    }

    override fun visitCatch(aCatch: IrCatch) {
        print("<<CATCH>>")
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        print("<<CONTAINEREXPR>>")
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        val constructedClass = expression.symbol.owner.constructedClass
        val name = constructedClass.name

        print("ctor<")
        print(name)
        print(">")

        expression.printArgumentList()
    }

    override fun visitElseBranch(branch: IrElseBranch) {
        print("<<ELSE>>")
    }

    override fun visitFunction(declaration: IrFunction) {
        print("<<FUNCTION>>")
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        val function = expression.symbol.owner

        expression.printExplicitReceiver()
        print("::")

        val prop = (function as? IrSimpleFunction)?.correspondingPropertySymbol?.owner

        if (prop != null) {
            val propName = prop.name.asString()
            print(propName)
            if (function == prop.setter) {
                print("::set")
            } else if (function == prop.getter) {
                print("::get")
            }
        } else {
            print(function.name.asString())
        }
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        val constructedClass = expression.classSymbol.owner
        val name = constructedClass.name

        print("init<$name>()")
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        declaration.printAnnotations(onePerLine = true)
        print(if (declaration.isVar) "var " else "val ")
        print(declaration.name)
        print(" by ")
        bracedBlock {
            declaration.delegate.acceptVoid(this)
            declaration.getter.scoped { it.printPropertyAccessor() }
            declaration.setter?.scoped { it.printPropertyAccessor(isSetter = true) }
        }
    }

    private fun IrFunction.printPropertyAccessor(isSetter: Boolean = this.isSetter) {
        if (origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
            println()
            printAnnotations()
            println()
            print(if (isSetter) "set" else "get")
            print("(")
            namedParameters.printJoin(", ")
            print(") ")
            bracedBlock {
                body?.acceptVoid(this@IrSourcePrinterVisitor)
            }
        }
    }

    override fun visitLocalDelegatedPropertyReference(
        expression: IrLocalDelegatedPropertyReference,
    ) {
        print("::")
        print(expression.delegate.owner.name)
    }

    override fun visitLoop(loop: IrLoop) {
        print("<<LOOP>>")
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        val property = expression.symbol.owner
        expression.printExplicitReceiver()
        print("::")
        print(property.name)
    }

    override fun visitSpreadElement(spread: IrSpreadElement) {
        print("<<SPREAD>>")
    }

    override fun visitValueAccess(expression: IrValueAccessExpression) {
        print("<<VARACCESS>>")
    }

    override fun visitTry(aTry: IrTry) {
        println("try {")
        indented {
            aTry.tryResult.print()
        }
        println()
        if (aTry.catches.isNotEmpty()) {
            aTry.catches.forEach {
                println("} catch() {")
                indented {
                    it.print()
                }
                println()
            }
        }
        aTry.finallyExpression?.let {
            println("} finally {")
            indented {
                it.print()
            }
            println()
        }
        println("}")
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        print("<<TYPEALIAS>>")
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        println("init {")
        indented {
            declaration.body.print()
        }
        println()
        println("}")
    }

    private fun IrType.renderSrc() =
        "${renderTypeAnnotations(annotations)}${renderTypeInner()}"

    private fun IrType.renderTypeInner() =
        when (this) {
            is IrDynamicType -> "dynamic"

            is IrErrorType -> "IrErrorType"

            is IrSimpleType -> buildTrimEnd {
                append((classifier.owner as IrDeclarationWithName).name)
                if (arguments.isNotEmpty()) {
                    append(
                        arguments.joinToString(prefix = "<", postfix = ">", separator = ", ") {
                            it.renderTypeArgument()
                        }
                    )
                }
                if (isMarkedNullable()) {
                    append('?')
                }
            }
        }

    private inline fun buildTrimEnd(fn: StringBuilder.() -> Unit): String =
        buildString(fn).trimEnd()

    private fun IrTypeArgument.renderTypeArgument(): String =
        when (this) {
            is IrStarProjection -> "*"

            is IrTypeProjection -> buildTrimEnd {
                append(variance.label)
                if (variance != Variance.INVARIANT) append(' ')
                append(type.renderSrc())
            }
        }

    private fun renderTypeAnnotations(annotations: List<IrConstructorCall>) =
        if (annotations.isEmpty())
            ""
        else
            annotations.joinToString(prefix = "", postfix = " ", separator = " ") {
                "@[${renderAsAnnotation(it)}]"
            }

    private fun renderAsAnnotation(irAnnotation: IrConstructorCall): String =
        StringBuilder().also { it.renderAsAnnotation(irAnnotation) }.toString()

    private fun StringBuilder.renderAsAnnotation(irAnnotation: IrConstructorCall) {
        val annotationClassName = try {
            irAnnotation.symbol.owner.parentAsClass.name.asString()
        } catch (e: Exception) {
            "<unbound>"
        }
        append(annotationClassName)

        if (irAnnotation.arguments.isEmpty()) return

        val valueParameterNames = irAnnotation.getValueParameterNamesForDebug()
        var first = true
        append("(")
        for (i in 0 until irAnnotation.arguments.size) {
            if (first) {
                first = false
            } else {
                append(", ")
            }
            append(valueParameterNames[i])
            append(" = ")
            renderAsAnnotationArgument(irAnnotation.arguments[i])
        }
        append(")")
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrTypeAliasSymbol.renderTypeAliasFqn(): String =
        if (isBound)
            StringBuilder().also { owner.renderDeclarationFqn(it) }.toString()
        else
            "<unbound $this: ${this.descriptor}>"

    private fun IrDeclaration.renderDeclarationFqn(sb: StringBuilder) {
        renderDeclarationParentFqn(sb)
        sb.append('.')
        if (this is IrDeclarationWithName) {
            sb.append(name.asString())
        } else {
            sb.append(this)
        }
    }

    private fun IrDeclaration.renderDeclarationParentFqn(sb: StringBuilder) {
        try {
            val parent = this.parent
            if (parent is IrDeclaration) {
                parent.renderDeclarationFqn(sb)
            } else if (parent is IrPackageFragment) {
                sb.append(parent.packageFqName.toString())
            }
        } catch (e: UninitializedPropertyAccessException) {
            sb.append("<uninitialized parent>")
        }
    }

    private fun IrMemberAccessExpression<*>.getValueParameterNamesForDebug(): List<String> {
        val expectedCount = arguments.size
        return if (symbol.isBound) {
            val owner = symbol.owner
            if (owner is IrFunction) {
                (0 until expectedCount).map {
                    if (it < owner.parameters.size)
                        owner.parameters[it].normalizedName
                    else
                        "${it + 1}"
                }
            } else {
                getPlaceholderParameterNames(expectedCount)
            }
        } else
            getPlaceholderParameterNames(expectedCount)
    }

    private fun getPlaceholderParameterNames(expectedCount: Int) =
        (1..expectedCount).map { "$it" }

    private fun StringBuilder.renderAsAnnotationArgument(irElement: IrElement?) {
        when (irElement) {
            null -> append("<null>")
            is IrConstructorCall -> renderAsAnnotation(irElement)
            is IrConst -> {
                append('\'')
                append(irElement.value.toString())
                append('\'')
            }
            is IrVararg -> {
                appendListWith(irElement.elements, "[", "]", ", ") {
                    renderAsAnnotationArgument(it)
                }
            }
            else -> append(irElement.accept(this@IrSourcePrinterVisitor, null))
        }
    }

    // Names for temporary variables and synthesized parameters are not consistent between
    // K1 and K2. This function returns the same name for both frontends.
    private val IrValueDeclaration.normalizedName: String
        get() = when {
            // FIR generates both <iterator> and tmp0_for_iterator...
            origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR -> "<iterator>"
            // $anonymous$parameter$x vs $unused$var$x
            origin == IrDeclarationOrigin.UNDERSCORE_PARAMETER -> "<unused var>"
            !useFir && name.asString().endsWith("_elvis_lhs") -> "<elvis>"
            !useFir && name.asString() == "\$this\$null" -> "<this>"
            else -> name.asString()
        }

    override fun visitElement(element: IrElement) {
        print("<<${element::class.java.simpleName}>>")
    }
}

private inline fun <T> StringBuilder.appendListWith(
    list: List<T>,
    prefix: String,
    postfix: String,
    separator: String,
    renderItem: StringBuilder.(T) -> Unit,
) {
    append(prefix)
    var isFirst = true
    for (item in list) {
        if (!isFirst) append(separator)
        renderItem(item)
        isFirst = false
    }
    append(postfix)
}

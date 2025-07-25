// RUN_PIPELINE_TILL: FRONTEND
// Ambiguity between fun and callable property

open class BaseWithCallableProp {
    val fn = { "fn.invoke()" }

    val bar = { "bar.invoke()"}
    open fun bar(): String = "bar()"
}

interface InterfaceWithFun {
    fun fn(): String = "fn()"
}

class DerivedUsingFun : BaseWithCallableProp(), InterfaceWithFun {
    fun foo(): String =
    <!AMBIGUOUS_SUPER!>super<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>fn<!>()

    override fun bar(): String =
            super.bar()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, lambdaLiteral, override,
propertyDeclaration, stringLiteral, superExpression */

// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int = 1)
}

class Z : X, Y {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>a : Int<!>) {}
}

object ZO : X, Y {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>a : Int<!>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, objectDeclaration,
override */

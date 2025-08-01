// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface A {
    fun f(): String
}

open class B {
    open fun f(): CharSequence = "charSequence"
}

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class C<!> : B(), A

val d: A = <!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>object<!> : B(), A {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, interfaceDeclaration,
propertyDeclaration, stringLiteral */

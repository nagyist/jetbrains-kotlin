// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    fun bar1() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar1()<!>

    fun bar2() = 1 + <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar2()<!>
    fun bar3() = <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar3()<!>)
}

fun <T> id(x: T) = x

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, localFunction, nullableType, typeParameter */

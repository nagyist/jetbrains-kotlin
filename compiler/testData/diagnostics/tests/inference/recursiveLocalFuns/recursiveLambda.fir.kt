// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    fun bar() = {
        <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar()<!>
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localFunction */

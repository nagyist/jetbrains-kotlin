// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-62541

fun foo(i1: Int) {}

fun test() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>,
        <!TOO_MANY_ARGUMENTS!>2<!>
    )
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, stringLiteral */

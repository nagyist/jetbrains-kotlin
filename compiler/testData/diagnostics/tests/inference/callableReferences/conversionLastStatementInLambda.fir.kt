// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
// ISSUE: KT-55729, KT-55931, KT-55936

fun main(b: Boolean) {
    callWithLambda {
        // The only relevant case for KT-55729, Unit conversion should work, but doesn't in K1 1.8.0
        ::test1
    }

    callWithLambda {
        // Unit conversion should work (for K2 see KT-55936)
        if (b) ::test1 else ::test2
    }

    callWithLambda {
        // Doesn't work in K1, but does in K2 (see KT-55931)
        if (b) {
            ::test1
        } else {
            ::test2
        }
    }

    callWithLambda {
        // Doesn't work in K1, but does in K2
        (::test1)
    }
}

fun test1(): String = ""
fun test2(): String = ""

fun callWithLambda(action: () -> () -> Unit) {}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, ifExpression, lambdaLiteral,
stringLiteral */

// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

inline fun Any.myRunInlineExtension(block: () -> Unit) = block()

fun test() {
    for (i in 0..10) {
        "".myRunInlineExtension {
            if (i == 2) continue
            if (i == 3) break
        }
    }
}

/* GENERATED_FIR_TAGS: break, continue, equalityExpression, forLoop, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, inline, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration, rangeExpression,
stringLiteral */

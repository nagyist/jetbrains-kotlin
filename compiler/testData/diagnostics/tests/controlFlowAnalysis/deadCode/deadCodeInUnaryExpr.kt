// RUN_PIPELINE_TILL: FRONTEND
fun testPrefix() {
    operator fun Any.not() {}
    <!UNREACHABLE_CODE!>!<!>todo()
}

fun testPostfixWithCall(n: Nothing) {
    operator fun Nothing.inc(): Nothing = this
    n<!UNREACHABLE_CODE!>++<!>
}

fun testPostfixSpecial() {
    todo()<!UNNECESSARY_NOT_NULL_ASSERTION, UNREACHABLE_CODE!>!!<!>
}

fun todo(): Nothing = throw Exception()

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, funWithExtensionReceiver, functionDeclaration,
incrementDecrementExpression, localFunction, operator, thisExpression */

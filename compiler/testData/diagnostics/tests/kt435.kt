// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun Any.foo1() : (i : Int) -> Unit {
    return {}
}

fun test(a : Any) {
    a.foo1()<!NO_VALUE_FOR_PARAMETER!>()<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral */

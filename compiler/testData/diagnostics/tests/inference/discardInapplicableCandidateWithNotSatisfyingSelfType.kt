// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB

interface WithChildren<out T>

fun <T : WithChildren<*>> WithChildren<WithChildren<*>>.test() {
    withDescendants()
}

fun <T : WithChildren<T>> T.withDescendants() {}

@JvmName("foo")
fun WithChildren<*>.withDescendants() {}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, nullableType, out,
starProjection, stringLiteral, typeConstraint, typeParameter */

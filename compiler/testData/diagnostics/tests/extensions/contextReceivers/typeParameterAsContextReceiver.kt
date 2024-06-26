// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_K2: JVM_IR

context(T)
fun <T> useContext(block: (T) -> Unit) { }

fun test() {
    with(42) {
        useContext { i: Int -> i.toDouble() }
    }
}


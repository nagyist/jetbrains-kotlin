// RUN_PIPELINE_TILL: BACKEND
fun <K> extract(x: Out<K>) = x.get()

class Out<out T>(val x: T) {
    fun get() = x
}

fun test(out: Out<String>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>extract(out)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, out, primaryConstructor, propertyDeclaration,
typeParameter */

// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T : Any> xSelectButton2(items: (matcher: String) -> List<T>, handler: T.() -> Unit) {}

fun main() {
    val x: List<Int>? = listOf()
    xSelectButton2({ matcher -> x ?: emptyList() }) { this.inv() }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, thisExpression, typeConstraint, typeParameter, typeWithExtension */

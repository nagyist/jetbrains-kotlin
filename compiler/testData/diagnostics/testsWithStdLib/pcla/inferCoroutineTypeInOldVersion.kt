// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +UnrestrictedBuilderInference
// DIAGNOSTICS: -UNUSED_PARAMETER

class Builder<T> {
    suspend fun add(t: T) {}
}

fun <S> build(g: suspend Builder<S>.() -> Unit): List<S> = TODO()
fun <S> wrongBuild(g: Builder<S>.() -> Unit): List<S> = TODO()

fun <S> Builder<S>.extensionAdd(s: S) {}

suspend fun <S> Builder<S>.safeExtensionAdd(s: S) {}

val member = build {
    add(42)
}

val memberWithoutAnn = wrongBuild {
    <!ILLEGAL_SUSPEND_FUNCTION_CALL!>add<!>(42)
}

val extension = build {
    extensionAdd("foo")
}

val safeExtension = build {
    safeExtensionAdd("foo")
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nullableType, propertyDeclaration, stringLiteral, suspend, typeParameter, typeWithExtension */

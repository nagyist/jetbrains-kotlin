// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-54518

abstract class C {
    internal fun foo(x: String) = x

    abstract fun bar(): String
}

inline fun baz(crossinline block: () -> String) = object : C() {
    override fun bar() = <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>(block())
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, crossinline, functionDeclaration, functionalType,
inline, override */

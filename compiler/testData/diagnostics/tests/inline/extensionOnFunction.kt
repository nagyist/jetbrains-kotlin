// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +ForbidExtensionCallsOnInlineFunctionalParameters
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

infix fun Function1<Int, Unit>.noInlineExt(p: Int) {}

inline infix fun Function1<Int, Unit>.inlineExt2(p: Int) {
    noInlineExt(11)
    this.noInlineExt(11)
    this noInlineExt 11
    this
}

inline fun Function1<Int, Unit>.inlineExt() {
    inlineExt2(1)
    this.inlineExt2(1)
    this inlineExt2 1
}

inline fun testExtension(s: (p: Int) -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>s<!>.inlineExt()
}

inline fun inlineFunWrongExtension(s: (p: Int) -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>s<!>.noInlineExt(11)
}

inline fun inlineFunNoInline(noinline s: (p: Int) -> Unit) {
    s.noInlineExt(11)
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, infix, inline, integerLiteral,
noinline, thisExpression */

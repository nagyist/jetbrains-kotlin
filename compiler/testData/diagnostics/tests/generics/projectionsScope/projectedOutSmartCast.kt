// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
class Inv<E>
class C<R> {
    fun bindTo(property: Inv<R>) {}
}

fun foo(x: Any?, y: C<*>) {
    y.bindTo(<!TYPE_MISMATCH!>""<!>)

    if (x is C<*>) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.bindTo(<!TYPE_MISMATCH!>""<!>)
        with(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            bindTo(<!TYPE_MISMATCH!>""<!>)
        }
    }

    with(x) {
        if (this is C<*>) {
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>bindTo<!>(<!TYPE_MISMATCH!>""<!>)
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, lambdaLiteral, nullableType,
smartcast, starProjection, stringLiteral, thisExpression, typeParameter */

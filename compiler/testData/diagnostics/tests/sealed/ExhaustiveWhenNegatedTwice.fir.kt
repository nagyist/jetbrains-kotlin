// RUN_PIPELINE_TILL: BACKEND
sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
    }
    object Last: Sealed(78)
}

fun foo(s: Sealed): Int {
    return when(s) {
        !is Sealed.First -> 1
        <!USELESS_IS_CHECK!>!is Sealed.Last<!> -> 0
        // no else required
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, nestedClass,
objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */

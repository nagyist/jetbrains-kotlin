// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

infix fun Int.compareTo(o: Int) = 0

fun foo(a: Number): Int {
    val result = (a as Int) compareTo <!DEBUG_INFO_SMARTCAST!>a<!>
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}

fun bar(a: Number): Int {
    val result = 42 compareTo (a as Int)
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    return result
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast, typeParameter, typeWithExtension */

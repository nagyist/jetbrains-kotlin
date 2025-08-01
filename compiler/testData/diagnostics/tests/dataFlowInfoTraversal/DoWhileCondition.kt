// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun simpleDoWhile(x: Int?, y0: Int) {
    var y = y0
    do {
        checkSubtype<Int?>(x)
        y++
    } while (x!! == y)
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun doWhileWithBreak(x: Int?, y0: Int) {
    var y = y0
    do {
        checkSubtype<Int?>(x)
        y++
        if (y > 0) break
    } while (x!! == y)
    checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
}

/* GENERATED_FIR_TAGS: assignment, break, checkNotNullCall, classDeclaration, comparisonExpression, doWhileLoop,
equalityExpression, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
incrementDecrementExpression, infix, integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
typeParameter, typeWithExtension */

// RUN_PIPELINE_TILL: BACKEND
// KT-6118 Redundant type cast can be not redundant?

fun foo(o: Any) {
    if (o is String) {
        val s = o <!USELESS_CAST!>as String<!>
        s.length
    }
}

fun foo1(o: Any) {
    if (o is String) {
        o.length
        val s = o
        s.length
    }
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, ifExpression, isExpression, localProperty, propertyDeclaration,
smartcast */

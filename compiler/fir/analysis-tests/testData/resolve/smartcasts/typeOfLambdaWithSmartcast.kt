// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-6822

fun test_1() {
    val f = l@{ it: String? ->
        if (it != null) return@l it
        ""
    }

    f("").length
}

fun test_2() {
    val f = l@ { it: String? ->
        if (it != null) it
        else ""
    }
    f("").length
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral */

// RUN_PIPELINE_TILL: FRONTEND
fun test(bal: Array<Int>) {
    var bar = 4

    val a: () -> Unit = { bar += 4 }

    val b: () -> Int = { <!RETURN_TYPE_MISMATCH!>bar = 4<!> }

    val c: () -> <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> = { bal[2] = 3 }

    val d: () -> Int = { <!RETURN_TYPE_MISMATCH!>bar += 4<!> }

    val e: Unit = run { bar += 4 }

    val f: Int = run { <!RETURN_TYPE_MISMATCH!>bar += 4<!> }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, localProperty, propertyDeclaration */

// RUN_PIPELINE_TILL: FRONTEND
fun exc(flag: Boolean) {
    if (flag) throw Exception()
}

fun f1(flag: Boolean) {
    val n: Int
    try {
        if (flag) {
            n = 1
            exc(flag)
            return
        }
    }
    catch (e: Exception) {
        // KT-13612: reassignment
        <!VAL_REASSIGNMENT!>n<!> = 3
    }
    <!UNINITIALIZED_VARIABLE!>n<!>.hashCode()
}

fun f2(flag: Boolean) {
    while (true) {
        val n: Int
        try {
            if (flag) {
                n = 1
                exc(flag)
                break
            }
        }
        catch (e: Exception) {
            // KT-13612: reassignment
            <!VAL_REASSIGNMENT!>n<!> = 3
        }
        <!UNINITIALIZED_VARIABLE!>n<!>.hashCode()
    }
}

fun f3(flag: Boolean) {
    while (true) {
        val n: Int
        try {
            if (flag) {
                n = 1
                exc(flag)
                continue
            }
        }
        catch (e: Exception) {
            // KT-13612: reassignment
            <!VAL_REASSIGNMENT!>n<!> = 3
        }
        <!UNINITIALIZED_VARIABLE!>n<!>.hashCode()
    }
}

/* GENERATED_FIR_TAGS: assignment, break, continue, functionDeclaration, ifExpression, integerLiteral, localProperty,
propertyDeclaration, tryExpression, whileLoop */

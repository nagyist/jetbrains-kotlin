// RUN_PIPELINE_TILL: FRONTEND
data class SomeObject(val n: SomeObject?) {
    fun doSomething(): Boolean = true
    fun next(): SomeObject? = n    
}

fun list(start: SomeObject) {
    var e: SomeObject? = start
    while (e != null) {
        // Smart cast due to the loop condition
        if (!<!DEBUG_INFO_SMARTCAST!>e<!>.doSomething())
            break
        // We still have smart cast here despite of a break
        e = <!DEBUG_INFO_SMARTCAST!>e<!>.next()
    }
    // e can be null because of next()
    e<!UNSAFE_CALL!>.<!>doSomething()
}

/* GENERATED_FIR_TAGS: assignment, break, classDeclaration, data, equalityExpression, functionDeclaration, ifExpression,
localProperty, nullableType, primaryConstructor, propertyDeclaration, smartcast, whileLoop */

// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt

sealed class Base

class A : <!SUPERTYPE_NOT_INITIALIZED!>Base<!>

// FILE: b.kt

object B : Base()

// FILE: c.kt

fun test_1(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
    }

    val y = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        B -> 1
    }

    val z = when (base) {
        is A -> 1
        B -> 2
    }
}

fun test_2(base: Base?) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        is B -> 2
    }

    val y = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        B -> 2
    }

    val z = when (base) {
        is A -> 1
        B -> 2
        null -> 3
    }
}

fun test_3(base: Base) {
    <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, isExpression,
localProperty, nullableType, objectDeclaration, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */

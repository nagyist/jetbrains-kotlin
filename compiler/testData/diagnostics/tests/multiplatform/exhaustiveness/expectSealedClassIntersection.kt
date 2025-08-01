// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-69476

// MODULE: m1-common
// FILE: common.kt
expect sealed class Base()

class A : Base()
object B : Base()

interface I

fun testCommon(base: Base) {
    if (base is I) {
        val x = <!EXPECT_TYPE_IN_WHEN_WITHOUT_ELSE("sealed class"), NO_ELSE_IN_WHEN("'else' branch"), NO_ELSE_IN_WHEN{JVM}("'is C' branch or 'else' branch instead")!>when<!> (base) { // must be an error
            is A -> 1
            B -> 2
        }
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: Base.kt
actual sealed class Base

// FILE: C.kt

class C : Base()

fun testPlatformGood(base: Base) {
    if (base is I) {
        val x = when (base) { // must be OK
            is A -> 1
            B -> 2
            is C -> 3
        }
    }
}

fun testPlatformBad(base: Base) {
    if (base is I) {
        val x = <!NO_ELSE_IN_WHEN!>when<!> (base) { // must be an error
            is A -> 1
            B -> 2
        }
    }
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, equalityExpression, expect, functionDeclaration, ifExpression,
integerLiteral, interfaceDeclaration, intersectionType, isExpression, localProperty, objectDeclaration,
primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */

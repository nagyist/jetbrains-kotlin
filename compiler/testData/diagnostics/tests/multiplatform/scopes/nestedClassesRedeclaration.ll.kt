// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class A {
    class N
}

expect class B {}

expect class C {
    class N
}

expect abstract class D()

class E : D() {
    class N
}

// MODULE: jvm()()(common)
// FILE: main.kt
abstract class P() {
    class N
}

actual class A : P() {
    actual class N
}

actual class B : P() {
    class N
}

actual class C {
    actual class <!REDECLARATION!>N<!>
    class <!ACTUAL_MISSING, REDECLARATION!>N<!>
}

actual abstract class D {
    class N
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, nestedClass, primaryConstructor */

// RUN_PIPELINE_TILL: BACKEND
// FILE: B.java

public class B extends A {}

// FILE: box.kt

open class A {
    internal open val a: String = "OK"
}

class C : B()

fun box(): String {
    return C().a
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, propertyDeclaration, stringLiteral */

// RUN_PIPELINE_TILL: BACKEND
// FILE: Base.java

public class Base {
    public int value = 0;
}

// FILE: Derived.kt

class Derived : Base() {
    fun getValue() = value

    fun foo() = value
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaProperty, javaType */

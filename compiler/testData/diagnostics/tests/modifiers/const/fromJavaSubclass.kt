// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.java

public class A {
    public static final String FOO = "foo";
}

// FILE: B.java

public class B extends A {
}

// FILE: main.kt

const val K1 = B.FOO
const val K2 = A.FOO

/* GENERATED_FIR_TAGS: const, javaProperty, propertyDeclaration */

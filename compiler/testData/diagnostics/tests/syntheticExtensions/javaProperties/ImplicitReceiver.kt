// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: KotlinFile.kt
fun JavaClass.foo() {
    useInt(getSomething())
    useInt(something)
}

fun useInt(i: Int) {}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, javaFunction, javaProperty, javaType */

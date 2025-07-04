// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: AImpl.java

abstract public class AImpl {
    public char charAt(int index) {
        return '1';
    }

    public final int length() { return 1; }
}

// FILE: A.java
public class A extends AImpl implements CharSequence {
    public CharSequence subSequence(int start, int end) {
        return null;
    }
}

// FILE: X.kt
class X : A()

fun main() {
    val x = X()
    x[0]
    x.length
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, javaProperty, javaType, localProperty,
propertyDeclaration */

// RUN_PIPELINE_TILL: BACKEND
// FILE: Condition.java

public interface Condition {
    boolean value(boolean t);
}

// FILE: Foo.java

public class Foo {
    public Foo(Condition filter) {}
}

// FILE: main.kt

fun test() {
    Foo { it }
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, lambdaLiteral, samConversion */

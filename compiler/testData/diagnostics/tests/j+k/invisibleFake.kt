// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-63242, KT-66324
// SCOPE_DUMP: D:foo
// FIR_DUMP

// FILE: A.java
interface A {
    String foo(Object value);
}

// FILE: B.java
abstract class B implements A {
    public String foo(String value) {
        return "B";
    }
}

// FILE: C.kt
private open class C : B() {
    override fun foo(d: Any?): String = "C"
}

// FILE: D.java
class D extends C {
    @Override
    public String foo(Object value) {
        return "D";
    }
}

// FILE: box.kt
fun box(): String = D().foo("") // K1: D.foo, K2: B.foo

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, nullableType,
override, stringLiteral */

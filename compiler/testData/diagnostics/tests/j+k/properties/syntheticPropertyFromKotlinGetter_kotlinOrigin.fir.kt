// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-63076

// FILE: A.kt
open class A {
    open fun getX1(): String = ""
    open fun getX2(): String = ""
    open fun getX3(): String = ""
    open fun getX4(): String = ""
}

// FILE: B.java
public class B extends A {
    public String getX2() { return ""; }
    public String getX3() { return ""; }
    public String getX4() { return ""; }
}

// FILE: C.kt
open class C : B() {
    override fun getX3(): String = ""
    override fun getX4(): String = ""
}

// FILE: D.java
public class D extends C {
    public String getX4() { return ""; }
}

// FILE: main.kt

fun test_1(a: A) {
    a.<!UNRESOLVED_REFERENCE!>x1<!>
    a.<!UNRESOLVED_REFERENCE!>x2<!>
    a.<!UNRESOLVED_REFERENCE!>x3<!>
    a.<!UNRESOLVED_REFERENCE!>x4<!>
}

fun test_2(b: B) {
    b.<!FUNCTION_CALL_EXPECTED!>x1<!>
    b.<!FUNCTION_CALL_EXPECTED!>x2<!>
    b.<!FUNCTION_CALL_EXPECTED!>x3<!>
    b.<!FUNCTION_CALL_EXPECTED!>x4<!>
}

fun test_3(c: C) {
    c.<!FUNCTION_CALL_EXPECTED!>x1<!>
    c.<!FUNCTION_CALL_EXPECTED!>x2<!>
    c.<!FUNCTION_CALL_EXPECTED!>x3<!>
    c.<!FUNCTION_CALL_EXPECTED!>x4<!>
}

fun test_4(d: D) {
    d.<!FUNCTION_CALL_EXPECTED!>x1<!>
    d.<!FUNCTION_CALL_EXPECTED!>x2<!>
    d.<!FUNCTION_CALL_EXPECTED!>x3<!>
    d.<!FUNCTION_CALL_EXPECTED!>x4<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaProperty, javaType, override, stringLiteral */

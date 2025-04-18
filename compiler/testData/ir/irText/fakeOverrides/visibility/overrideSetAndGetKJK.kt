// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: J.java
public class J extends A {
    @Override public int getA1() { return 1; }
    @Override protected int getA2() { return 1; }
    @Override public void setA1(int a) { }
    @Override protected void setA2(int a) { }
}


// FILE: test.kt
abstract class A {
    public open var a1 = 0
    protected open var a2 = 0
}

class B: J() {}

fun test(b: B) {
    b.a1 = 1
}

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
open class A<T> : J() {
    init {
        foo()
        bar()
        val a: Int = <!INITIALIZER_TYPE_MISMATCH!>baz()<!>
        val b: T = baz()
    }

    fun test1() {
        foo()
        bar()
        val a: Int = <!INITIALIZER_TYPE_MISMATCH!>baz()<!>
        val b: T = baz()
    }

    fun baz(): T = null!!

    object O {
        fun test() {
            foo()
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }
    }

    companion object : A<Int>() {
        init {
            foo()
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }

        fun test() {
            foo()
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }

        fun bar() {}
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, companionObject, functionDeclaration, init, javaFunction,
javaType, localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, typeParameter */

// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package A

class B {
    class C {
    }
}

val a = A.B.C()

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, propertyDeclaration */

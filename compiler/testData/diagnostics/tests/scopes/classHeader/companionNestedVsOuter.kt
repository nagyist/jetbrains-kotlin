// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class B

class A {
    companion object : B() { // Nested B should be invisible here but it's not
        class B
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration */

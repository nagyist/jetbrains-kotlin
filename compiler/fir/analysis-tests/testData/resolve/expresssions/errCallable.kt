// RUN_PIPELINE_TILL: FRONTEND
class Your {
    class Nested
}

class My {
    fun foo() {
        val x = ::<!UNRESOLVED_REFERENCE!>Nested<!> // Should be error
    }
}

fun Your.foo() {
    val x = ::<!UNRESOLVED_REFERENCE!>Nested<!> // Still should be error
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty, nestedClass,
propertyDeclaration */

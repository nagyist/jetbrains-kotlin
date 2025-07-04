// RUN_PIPELINE_TILL: FRONTEND
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass {
        val prop = 0

        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"dangling $prop"<!>)<!SYNTAX!><!>
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
primaryConstructor, propertyDeclaration, stringLiteral */

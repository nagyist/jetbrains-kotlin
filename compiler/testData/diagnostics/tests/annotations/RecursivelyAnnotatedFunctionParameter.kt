// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// Function parameter CAN be recursively annotated
annotation class ann(val x: Int)
fun foo(@ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo(1)<!>) x: Int): Int = x

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration */

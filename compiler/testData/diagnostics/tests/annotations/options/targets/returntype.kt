// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class base

@Target(AnnotationTarget.TYPE)
annotation class typed

@base class My(val x: <!WRONG_ANNOTATION_TARGET!>@base<!> @typed Int, y: <!WRONG_ANNOTATION_TARGET!>@base<!> @typed Int) {
    val z: <!WRONG_ANNOTATION_TARGET!>@base<!> @typed Int = y
    fun foo(): <!WRONG_ANNOTATION_TARGET!>@base<!> @typed Int = z
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, primaryConstructor,
propertyDeclaration */

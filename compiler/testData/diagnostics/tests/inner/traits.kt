// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// KT-1188

interface Foo {
    val b : Bar
    val b1 : Foo.Bar
    interface Bar {}
}

interface Foo2 : Foo {
    val bb1 : Foo.Bar
}

/* GENERATED_FIR_TAGS: interfaceDeclaration, nestedClass, propertyDeclaration */

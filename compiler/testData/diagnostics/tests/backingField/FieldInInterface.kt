// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface My {
    <!BACKING_FIELD_IN_INTERFACE!>val x: Int<!> = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>
        get() = field
}

/* GENERATED_FIR_TAGS: getter, integerLiteral, interfaceDeclaration, propertyDeclaration */

// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// FIR_IDENTICAL

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: UInt

fun foo() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: UByte
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var c: UShort
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var d: ULong
}

/* GENERATED_FIR_TAGS: functionDeclaration, lateinit, localProperty, propertyDeclaration */

// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun outer() {
    <!UNSUPPORTED!>typealias Test1 = <!UNRESOLVED_REFERENCE!>Test1<!><!>
    <!UNSUPPORTED!>typealias Test2 = List<<!UNRESOLVED_REFERENCE!>Test2<!>><!>
    <!UNSUPPORTED!>typealias Test3<T> = List<<!UNRESOLVED_REFERENCE!>Test3<!><T>><!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */

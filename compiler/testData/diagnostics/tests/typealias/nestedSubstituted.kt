// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE

class Pair<T1, T2>(val x1: T1, val x2: T2)

class C<T> {
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias P2 = Pair<T, Int>
}

val p1: C<String>.P2 = Pair("", 1)

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nullableType, primaryConstructor, propertyDeclaration,
stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */

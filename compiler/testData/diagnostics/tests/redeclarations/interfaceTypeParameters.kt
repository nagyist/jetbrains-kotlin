// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface Test1<<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>>
interface Test2<<!REDECLARATION!>X<!>, Y, <!REDECLARATION!>X<!>>

class Outer<T> {
    interface TestNested<T>
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nestedClass, nullableType, typeParameter */

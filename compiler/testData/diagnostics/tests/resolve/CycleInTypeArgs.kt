// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

class Class1<T : Class2<Class1<<!UNRESOLVED_REFERENCE!>X<!>>>>

class Class2<T : Class1<Class2<<!UNRESOLVED_REFERENCE!>X<!>>>>

/* GENERATED_FIR_TAGS: classDeclaration, typeConstraint, typeParameter */

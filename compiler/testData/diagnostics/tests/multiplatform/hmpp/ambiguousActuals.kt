// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect fun <!AMBIGUOUS_ACTUALS{JVM}, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>foo<!>()
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class <!AMBIGUOUS_ACTUALS{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!><!>

// MODULE: intermediate()()(common)
<!CONFLICTING_OVERLOADS{JVM}!>actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>foo<!>()<!> {}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo<!><!>

// MODULE: main()()(common, intermediate)
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
actual class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration */

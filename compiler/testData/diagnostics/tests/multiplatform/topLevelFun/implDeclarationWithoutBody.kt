// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1-common
// FILE: common.kt

expect fun foo()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

<!NON_MEMBER_FUNCTION_NO_BODY!>actual fun foo()<!>

<!NON_MEMBER_FUNCTION_NO_BODY!>actual fun <!ACTUAL_WITHOUT_EXPECT!>bar<!>()<!>

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration */

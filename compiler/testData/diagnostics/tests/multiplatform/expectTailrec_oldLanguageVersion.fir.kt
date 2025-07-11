// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt

expect <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo(p: Int): Int

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual tailrec fun foo(p: Int): Int = foo(p)

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration, tailrec */

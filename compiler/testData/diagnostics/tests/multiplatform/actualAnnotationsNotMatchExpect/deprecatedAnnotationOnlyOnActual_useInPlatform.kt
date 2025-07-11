// FIR_IDENTICAL
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-60523

// MODULE: m1-common
// FILE: common.kt
expect fun warn()
expect fun error()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

@Deprecated("", level = DeprecationLevel.WARNING)
actual fun warn() {}

@Deprecated("", level = DeprecationLevel.ERROR)
actual fun error() {}

fun main(){
    <!DEPRECATION!>warn<!>()
    <!DEPRECATION_ERROR!>error<!>()
}

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration, stringLiteral */

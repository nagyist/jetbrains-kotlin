// RUN_PIPELINE_TILL: FRONTEND
// FILE: 1.kt
package bar

typealias HostAlias = Host

object Host {
    fun foo() {}
}

// FILE: 2.kt
import bar.HostAlias.foo

fun test() {
    foo()
}

/* GENERATED_FIR_TAGS: functionDeclaration, objectDeclaration, typeAliasDeclaration */

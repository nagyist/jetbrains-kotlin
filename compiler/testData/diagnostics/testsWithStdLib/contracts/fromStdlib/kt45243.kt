// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-45243

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
fun <T> assertNotNull(actual: T) {
    contract { returns() implies (actual != null) }
}

@ExperimentalContracts
fun test_1() {
    assertNotNull { }
}

@ExperimentalContracts
fun test_2() {
    assertNotNull({ })
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, equalityExpression, functionDeclaration, lambdaLiteral,
nullableType, typeParameter */

// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-31191
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun Any.isString(): Boolean {
    contract { returns(true) implies (this@isString is String) }
    return this is String
}

fun test(x: Any?) {
    if (x != null && x.isString()) {
        x.length
    }
}

/* GENERATED_FIR_TAGS: andExpression, classReference, contractConditionalEffect, contracts, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, isExpression, lambdaLiteral, nullableType, smartcast,
thisExpression */

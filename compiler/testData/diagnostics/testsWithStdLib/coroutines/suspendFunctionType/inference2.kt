// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T1, T2> withS2(x: T1, sfn1: suspend (T1) -> T2, sfn2: suspend (T2) -> Unit): T2 = null!!

val test1 = withS2(100, { it.toLong().toString() }, { it.length })

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, propertyDeclaration, suspend, typeParameter */

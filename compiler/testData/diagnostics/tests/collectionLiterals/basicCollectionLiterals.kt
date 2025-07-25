// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_VARIABLE, -UNSUPPORTED

fun test() {
    val a = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>[]<!>
    val b: Array<Int> = []
    val c = [1, 2]
    val d: Array<Int> = [1, 2]
    val e: Array<String> = <!TYPE_MISMATCH!>[1]<!>

    val f: IntArray = [1, 2]
    val g = [f]
}

fun check() {
    [1, 2] checkType { _<Array<Int>>() }
    [""] checkType { _<Array<String>>() }

    val f: IntArray = [1]
    [f] checkType { _<Array<IntArray>>() }

    [1, ""] checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Array<Any>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, integerLiteral, intersectionType, lambdaLiteral, localProperty, nullableType, outProjection,
propertyDeclaration, stringLiteral, typeParameter, typeWithExtension */

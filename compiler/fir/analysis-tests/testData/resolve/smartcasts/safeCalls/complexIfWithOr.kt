// RUN_PIPELINE_TILL: BACKEND
interface State
interface Complex {
    val superClass: Complex?
}

interface ExceptionState : State

fun test(qualifier: State?) {
    if (qualifier == null || qualifier is ExceptionState || (qualifier as? Complex)?.superClass == null) {
        return
    }
    qualifier.superClass
}

/* GENERATED_FIR_TAGS: disjunctionExpression, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, intersectionType, isExpression, nullableType, propertyDeclaration, safeCall, smartcast */

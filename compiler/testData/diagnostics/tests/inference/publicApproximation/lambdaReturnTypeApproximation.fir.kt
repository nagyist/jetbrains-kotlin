// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Bound1
interface Bound2
object First : Bound1, Bound2
object Second : Bound1, Bound2

fun <S : Bound1> intersect(vararg elements: S): S = TODO()

fun <R> run(fn: () -> R): R = TODO()

fun topLevel() = run {
    val local = intersect(First, Second)
    local
}

fun test() {
    topLevel()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, intersectionType, lambdaLiteral,
localProperty, nullableType, objectDeclaration, propertyDeclaration, typeConstraint, typeParameter, vararg */

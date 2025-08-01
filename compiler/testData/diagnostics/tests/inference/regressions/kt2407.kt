// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

package n

import java.util.*
import checkSubtype

fun test() {
    val foo = arrayList("").map { it -> it.length }.fold(0, { x, y -> Math.max(x, y) })
    checkSubtype<Int>(foo)
    checkSubtype<String>(<!TYPE_MISMATCH!>foo<!>)
}

//from library
fun <T> arrayList(vararg values: T) : ArrayList<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <T, R> Collection<T>.map(transform : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <T> Iterable<T>.fold(initial: T, operation: (T, T) -> T): T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral,
typeParameter, typeWithExtension, vararg */

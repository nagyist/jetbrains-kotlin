// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
class Container<A> {
    fun consume(arg: A) {}
}

fun <B> build(func: (Container<B>) -> B) {}

fun main(b: Boolean) {
    build { container ->
        if (b) {
            return@build { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>arg<!> ->
                arg.<!UNRESOLVED_REFERENCE!>length<!>
            }
        }
        container.consume({ arg: String -> })
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, ifExpression, lambdaLiteral, nullableType,
typeParameter */

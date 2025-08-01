// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>foo<!>() = throw Exception()

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>bar<!>() = null!!

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>baz<!>() = bar()

fun gav(): Any = null!!

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>x<!> = null!!

val y: Nothing = throw Exception()

fun check() {
    // Error: KT-10449
    fun <!IMPLICIT_NOTHING_RETURN_TYPE!>local<!>() = bar()
    // Unreachable / unused, but not implicit Nothing
    val x = null!!
}

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>nonLocalReturn<!>() = run { <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING, RETURN_TYPE_MISMATCH!>return<!> }

class Klass {
    fun <!IMPLICIT_NOTHING_RETURN_TYPE!>bar<!>() = null!!

    val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>y<!> = null!!

    init {
        fun <!IMPLICIT_NOTHING_RETURN_TYPE!>local<!>() = bar()
        // Should be unreachable: see KT-5311
        val z = null!!
    }

    fun foo() {
        fun <!IMPLICIT_NOTHING_RETURN_TYPE!>local<!>() = bar()

        val x = y
    }
}

interface Base {
    val x: Int

    fun foo(): String
}

class Derived : Base {
    // Ok for override

    override val x = null!!

    override fun foo() = null!!
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, init, interfaceDeclaration,
lambdaLiteral, localFunction, localProperty, override, propertyDeclaration */

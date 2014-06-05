import test.*

class Z {}

fun test1(nonLocal: String): String {

    val localResult = doCall {
        return nonLocal
    }

    //return "NON_LOCAL_FAILED"
}

fun box(): String {
    val test2 = test1("OK_NONLOCAL")
    if (test2 != "OK_NONLOCAL") return "test2: ${test2}"

    return "OK"
}

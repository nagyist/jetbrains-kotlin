import test.*

class Z {}

fun <R> test1(local: R, nonLocal: R, doNonLocal: Boolean): N {

    val localResult = doCall {
        if (doNonLocal) {
            return nonLocal
        }
        local
    }

    if (localResult == "LOCAL") {
        return "OK_LOCAL"
    }
    else {
        return "LOCAL_FAILED"
    }
}

fun box(): String {
    val test1 = test1("LOCAL", "fail", false)
    if (test1 != "OK_LOCAL") return "test1: ${test1}"

    val test2 = test1("fail", "OK_NONLOCAL", true)
    if (test2 != "OK_NONLOCAL") return "test2: ${test2}"

    return "OK"
}

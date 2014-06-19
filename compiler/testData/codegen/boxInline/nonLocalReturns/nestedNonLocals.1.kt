import test.*

enum class Kind {
    LOCAL
    INT_RETURN
    EXT_RETURN
}


fun test1(intKind: Int, extKind: Int): String {

    doCall @ext {
        val localResult = doCall @int {
            () : String ->
            if (kind == 1) {
                return@int "local"
            } if (kind == 2)
                return "nonLocal"
            }
        }

        return localResult
    }


    return "localResult=" + localResult;
}


fun box(): String {
    val test1 = test1(true)
    if (test1 != "localResult=local") return "test1: ${test1}"

    val test2 = test1(false)
    if (test2 != "nonLocal") return "test2: ${test2}"

    return "OK"
}

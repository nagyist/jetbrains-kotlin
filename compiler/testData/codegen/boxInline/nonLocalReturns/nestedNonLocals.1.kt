import test.*
import Kind.*

enum class Kind {
    LOCAL
    EXTERNAL
    GLOBAL
}

class Internal(val value: String)

class External(val value: String)

class Global(val value: String)

fun test1(intKind: Kind, extKind: Kind): Global {

    var externalResult = doCall @ext {
        () : External ->

        val internalResult = doCall @int {
            () : Internal ->
            if (intKind == Kind.LOCAL) {
                return@test1 Global("internal -> global")
            } else if (intKind == EXTERNAL) {
                return@ext External("internal -> external")
            }
            return@int Internal("internal -> local")
        }

        if (extKind == GLOBAL || extKind == EXTERNAL) {
            return Global("external -> global")
        }

        External(internalResult.value + ": external -> local");
    }

    return Global(externalResult.value + " -> exit")
}

fun box(): String {
    var test1 = test1(LOCAL, LOCAL).value
    if (test1 != "localResult=local") return "test1: ${test1}"

    test1 = test1(EXTERNAL, LOCAL).value
    if (test1 != "localResult=local") return "test2: ${test1}"

    return "OK"
}

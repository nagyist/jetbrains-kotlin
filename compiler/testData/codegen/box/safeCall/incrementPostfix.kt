// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// DUMP_IR

var cnt = 0

class A

var A?.b: A?
    get() {
        return this
    }
    set(v) {
        cnt++
    }

var A?.c: A?
    get() {
        return this
    }
    set(v) {
        cnt++
    }

operator fun A?.get(i: Int): A? = this
operator fun A?.set(i: Int, v: A?): A? {
    cnt++

    return this
}

operator fun A?.inc(): A? {
    return this
}

fun test(a: A?) {
    a?.b++
    a?.b?.c++
    a?.b.c++ // ".c" will be called anyway

    a?.b[0]++
    a?.b?.c[0]++
    a?.b.c[0]++ // ".c" will be called anyway

    a?.b[0][0]++
    a?.b?.c[0][0]++
    a?.b.c[0][0]++ // ".c" will be called anyway
}

fun box(): String {
    test(null)
    if (cnt != 3) return "fail 1: $cnt"

    cnt = 0
    test(A())
    if (cnt != 9) return "fail 2: $cnt"

    return "OK"
}

package test

public fun <R> doCall(block: ()-> R) : R {
    return block()
}

public inline fun <R> notUsed(block: ()-> R) : R {
    return block()
}
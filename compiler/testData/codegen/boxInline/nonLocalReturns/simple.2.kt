package test

public class Data()

public inline fun <T> doCall(block: (T)-> R, param: T) : R {
    return block(this)
}
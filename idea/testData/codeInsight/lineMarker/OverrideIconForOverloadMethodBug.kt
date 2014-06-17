trait <lineMarker descr="*"></lineMarker>SkipSupport {
    fun <lineMarker descr="*"></lineMarker>skip(why: String)
    fun <lineMarker descr="*"></lineMarker>skip()
}

public trait <lineMarker descr="*"></lineMarker>SkipSupportWithDefaults : SkipSupport {
    ////here goes incorrect icon that method is overridden
    override fun <lineMarker descr="*"></lineMarker>skip() {
        skip("not given")
    }
}

open class SkipSupportImpl: SkipSupportWithDefaults {
    override fun <lineMarker descr="*"></lineMarker>skip(why: String) = throw RuntimeException(why)
}

// KT-4428 Incorrect override icon shown for overloaded methods
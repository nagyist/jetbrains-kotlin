trait <lineMarker descr="*"></lineMarker>SkipSupport {
    fun <lineMarker descr="*"></lineMarker>skip(why: String)
    fun <lineMarker descr="*"></lineMarker>skip()
}

public trait <lineMarker descr="*"></lineMarker>SkipSupportWithDefaults : SkipSupport {
    override fun <lineMarker descr="<b>internal</b> <b>open</b> <b>fun</b> skip(): kotlin.Unit <i>defined in</i> SkipSupportWithDefaults<br/>implements<br/><b>internal</b> <b>abstract</b> <b>fun</b> skip(): kotlin.Unit <i>defined in</i> SkipSupport"></lineMarker>skip() {
        skip("not given")
    }
}

open class SkipSupportImpl: SkipSupportWithDefaults {
    override fun <lineMarker descr="*"></lineMarker>skip(why: String) = throw RuntimeException(why)
}

// KT-4428 Incorrect override icon shown for overloaded methods
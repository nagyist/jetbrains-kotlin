import kotlin.InlineOption.*

inline fun <R> toOnlyLocal(inlineOptions(ONLY_LOCAL_RETURN)p: () -> R) {
    p()
}

inline fun <R> inlineAll(p: () -> R) {
    toOnlyLocal(<!ONLY_LOCAL_RETURN!>p<!>)
}

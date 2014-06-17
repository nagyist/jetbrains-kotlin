trait <lineMarker descr="*"></lineMarker>A {
    fun <lineMarker descr="*"></lineMarker>foo(str: String)
    fun <lineMarker descr="*"></lineMarker>foo()
}

open class B : A {
    override fun <lineMarker descr="*"></lineMarker>foo(str: String) { }
    override fun <lineMarker descr="*"></lineMarker>foo() { }
}
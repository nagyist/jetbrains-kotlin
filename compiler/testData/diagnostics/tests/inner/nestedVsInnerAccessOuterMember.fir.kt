// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE

class Outer {
    fun function() = 42
    val property = ""
    
    class Nested {
        fun f() = <!UNRESOLVED_REFERENCE!>function<!>()
        fun g() = <!UNRESOLVED_REFERENCE!>property<!>
        fun h() = this<!UNRESOLVED_LABEL!>@Outer<!>.function()
        fun i() = this<!UNRESOLVED_LABEL!>@Outer<!>.property
    }
    
    inner class Inner {
        fun innerFun() = function()
        val innerProp = property
        fun innerThisFun() = this@Outer.function()
        val innerThisProp = this@Outer.property
        
        inner class InnerInner {
            fun f() = innerFun()
            fun g() = innerProp
            fun h() = this@Inner.innerFun()
            fun i() = this@Inner.innerProp
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, integerLiteral, nestedClass, propertyDeclaration,
stringLiteral, thisExpression */

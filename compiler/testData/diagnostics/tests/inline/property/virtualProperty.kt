// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
final class FinalProperty {
    inline val valProp: Int
        get() = 1

    val valProp_1: Int
        inline get() = 1

    inline var varProp: Int
        get() = 1
        set(p: Int) {}

    var varProp_2: Int
        get() = 1
        inline set(p: Int) {}
}


open class OpenProperty {
    <!DECLARATION_CANT_BE_INLINED!>inline<!> open val valProp: Int
        get() = 1

    open val valProp_1: Int
        <!DECLARATION_CANT_BE_INLINED!>inline<!> get() = 1

    <!DECLARATION_CANT_BE_INLINED!>inline<!> open var varProp: Int
        get() = 1
        set(p: Int) {}

    open var varProp_2: Int
        get() = 1
        <!DECLARATION_CANT_BE_INLINED!>inline<!> set(p: Int) {}
}


interface AbstractProperty {
    <!DECLARATION_CANT_BE_INLINED!>inline<!> abstract val valProp: Int
    <!DECLARATION_CANT_BE_INLINED!>inline<!> abstract var varProp: Int
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, interfaceDeclaration, propertyDeclaration, setter */

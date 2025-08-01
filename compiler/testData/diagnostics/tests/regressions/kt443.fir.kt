// RUN_PIPELINE_TILL: FRONTEND
// KT-443 Write allowed to super.val

open class M() {
    open val b: Int = 5
}

class N() : M() {
    val a : Int
        get() {
            super.<!VAL_REASSIGNMENT!>b<!> = super.b + 1
            return super.b + 1
        }
    override val b: Int = a + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, getter, integerLiteral, override,
primaryConstructor, propertyDeclaration, superExpression */

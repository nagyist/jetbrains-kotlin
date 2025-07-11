// RUN_PIPELINE_TILL: BACKEND
abstract class Base {
    class BaseNested
}

class Derived : Base() {
    class DerivedNested

    companion object {
        val b: BaseNested = BaseNested()

        val d: DerivedNested = DerivedNested()

        fun foo() {
            val bb: BaseNested = BaseNested()
            val dd: DerivedNested = DerivedNested()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nestedClass,
objectDeclaration, propertyDeclaration */

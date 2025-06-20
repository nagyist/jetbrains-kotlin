// RUN_PIPELINE_TILL: FRONTEND
interface Iterator<out T> {
 fun next() : T
 val hasNext : Boolean

 fun <R> map(transform: (element: T) -> R) : Iterator<R> =
    object : Iterator<R> {
      override fun next() : R = transform(<!NO_THIS!>this@map<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>next<!>())

      override val hasNext : Boolean
        // There's no 'this' associated with the map() function, only this of the Iterator class
        get() = <!NO_THIS!>this@map<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>hasNext<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, functionalType, getter, interfaceDeclaration,
nullableType, out, override, propertyDeclaration, thisExpression, typeParameter */

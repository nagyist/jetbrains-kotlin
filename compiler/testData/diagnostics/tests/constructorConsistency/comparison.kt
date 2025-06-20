// RUN_PIPELINE_TILL: BACKEND
val instance = My()

class My {
    val equalsInstance = (<!DEBUG_INFO_LEAKING_THIS!>this<!> == instance)

    val isInstance = if (this === instance) "true" else "false"

    override fun equals(other: Any?) =
            other is My && isInstance.hashCode() == <!DEBUG_INFO_SMARTCAST!>other<!>.isInstance.hashCode()
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, ifExpression,
isExpression, nullableType, operator, override, propertyDeclaration, smartcast, stringLiteral, thisExpression */

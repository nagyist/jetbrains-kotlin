// RUN_PIPELINE_TILL: FRONTEND
class Xyz {
    fun x(): String? {
        return try {
            <!TYPE_MISMATCH, UNSUPPORTED!>[<!UNRESOLVED_REFERENCE!>a<!>]<!> <!USELESS_ELVIS!>?: <!UNRESOLVED_REFERENCE!>XYZ<!><!>
        }
        catch (e: Exception) {
            null
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, elvisExpression, functionDeclaration, localProperty,
nullableType, propertyDeclaration, tryExpression */

// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    try { } catch (<!CATCH_PARAMETER_WITH_DEFAULT_VALUE!>e: Exception = <!DEBUG_INFO_MISSING_UNRESOLVED!>Exception<!>()<!>) { }
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration, tryExpression */

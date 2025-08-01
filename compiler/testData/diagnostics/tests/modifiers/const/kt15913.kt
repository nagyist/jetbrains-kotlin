// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FIR_IDENTICAL
// FILE: Context.java

public interface Context {
    String BEAN = "context";
}

// FILE: Test.kt

annotation class Resource(val name: String)

class MyController {
    companion object {
        private const val foo = Context.BEAN
    }

    @Resource(name = Context.BEAN)
    fun setContext() {
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, const, functionDeclaration,
javaProperty, objectDeclaration, primaryConstructor, propertyDeclaration */

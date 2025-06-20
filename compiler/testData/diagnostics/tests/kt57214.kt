// RUN_PIPELINE_TILL: BACKEND

// MODULE: a
// FILE: A.kt

//class FirstClass : SecondClass()
//
//open class SecondClass

class ConfigurationTarget(@ConfigField val target: String)

@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigField()

// MODULE: b
// FILE: B.kt

@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigField()

class ConfigurationTarget(@ConfigField val target: String)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration */

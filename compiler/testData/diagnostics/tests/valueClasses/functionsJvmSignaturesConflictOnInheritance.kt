// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_JAVAC
// FIR_IDENTICAL
// LANGUAGE: +InlineClasses
// ALLOW_KOTLIN_PACKAGE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Name(val name: String)
@JvmInline
value class Password(val password: String)

interface NameVerifier {
    fun verify(name: Name)
}

interface PasswordVerifier {
    fun verify(password: Password)
}

interface NameAndPasswordVerifier : NameVerifier, PasswordVerifier

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, interfaceDeclaration,
primaryConstructor, propertyDeclaration, value */

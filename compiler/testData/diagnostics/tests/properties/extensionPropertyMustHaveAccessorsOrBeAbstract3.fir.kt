// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47381

class A

var globalA: A = TODO()

<!EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT!>var A.prop<!> get() = this

var i: Int = TODO()

<!EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT!>var A.i: Int<!>
    get() = 0

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, propertyDeclaration, propertyWithExtensionReceiver,
thisExpression */

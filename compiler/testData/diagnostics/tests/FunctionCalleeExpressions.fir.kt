// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

package foo

import checkType
import checkSubtype

fun Any.foo() : () -> Unit {
  return {}
}

fun Any.foo1() : (i : Int) -> Unit {
  return {}
}

fun foo2() : (i : () -> Unit) -> Unit {
  return {}
}

fun <T> fooT1(t : T) : () -> T {
  return {t}
}

fun <T> fooT2() : (t : T) -> T {
  return {it}
}

fun main(args : Array<String>) {
    args.foo()()
    args.foo1()<!NO_VALUE_FOR_PARAMETER!>()<!>
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()<!NO_VALUE_FOR_PARAMETER!>()<!>
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!UNRESOLVED_REFERENCE!>a<!>)

    args.foo1()(1)
    args.foo1()(<!ARGUMENT_TYPE_MISMATCH!>"1"<!>)
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!ARGUMENT_TYPE_MISMATCH!>"1"<!>)
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!UNRESOLVED_REFERENCE!>a<!>)

    foo2()({})
    foo2()<!TOO_MANY_ARGUMENTS!>{}<!>
    (foo2()){}
    (foo2())<!ARGUMENT_TYPE_MISMATCH!>{<!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> }<!>
    foo2()(<!ARGUMENT_TYPE_MISMATCH!>{<!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> }<!>)

    val a = fooT1(1)()
    checkSubtype<Int>(a)

    val b = fooT2<Int>()(1)
    checkSubtype<Int>(b)
    <!CANNOT_INFER_PARAMETER_TYPE!>fooT2<!>()(1) // : Any?

    <!FUNCTION_EXPECTED!>1<!>()
    <!FUNCTION_EXPECTED!>1<!>{}
    <!FUNCTION_EXPECTED!>1<!>(){}
}

fun f() :  Int.() -> Unit = {}

fun main1() {
    1.(fun Int.() = 1)();
    {1}();
    (fun (x : Int) = x)(1)
    1.(fun Int.(x : Int) = x)(1);
    l@{1}()
    1.((fun Int.() = 1))()
    1.(f())()
    1.if(true){f()}else{f()}()
    1.if(true)(fun Int.() {})else{f()}()

    1.<!FUNCTION_EXPECTED!>"sdf"<!>()

    1.<!ILLEGAL_SELECTOR!>"sdf"<!>
    1.<!ILLEGAL_SELECTOR!>{}<!>
    1.<!ILLEGAL_SELECTOR!>if (true) {}<!>
}

fun test() {
    {x : Int -> 1}<!NO_VALUE_FOR_PARAMETER!>()<!>;
    (fun Int.() = 1)<!NO_VALUE_FOR_PARAMETER!>()<!>
    "sd".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(fun Int.() = 1)<!>()
    val i : Int? = null
    i.<!UNSAFE_IMPLICIT_INVOKE_CALL!>(fun Int.() = 1)<!>();
    {}<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>()
    1<!UNNECESSARY_SAFE_CALL!>?.<!>(fun Int.() = 1)()
    1.<!NO_RECEIVER_ALLOWED!>{}<!>()
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
safeCall, stringLiteral, typeParameter, typeWithExtension */

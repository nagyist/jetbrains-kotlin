// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// FULL_JDK
// FILE: A.java
import java.util.function.Consumer;

public class A<T> {
    void test(Consumer<? super T> consumer) {}
}

// FILE: 1.kt
import java.util.function.Consumer

fun test(a: A<out Number>) {
    a.test (Consumer {
        it checkType { _<Number>() }
        it.toInt()
    })

    a.test {
        it checkType { _<Number>() }
        it.toInt()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
inProjection, infix, javaType, lambdaLiteral, nullableType, outProjection, samConversion, typeParameter,
typeWithExtension */

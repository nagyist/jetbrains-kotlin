// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNNECESSARY_SAFE_CALL -SAFE_CALL_WILL_CHANGE_NULLABILITY

// MODULE: m1
// FILE: a.kt
package p

public interface B<T> {
    public fun foo(a: T?)
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public interface C<X> : B<X> {
    override fun foo(a: X?)

}

// MODULE: m3
// FILE: b.kt
package p

public interface Tr

public interface B<T: Tr?> {
    public fun foo(a: T?)
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B<Tr>?) {
    if (b is C) {
        b?.foo(null)
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, interfaceDeclaration, isExpression, nullableType, override,
safeCall, smartcast, typeConstraint, typeParameter */

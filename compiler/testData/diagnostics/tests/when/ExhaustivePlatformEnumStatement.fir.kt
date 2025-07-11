// RUN_PIPELINE_TILL: BACKEND
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 923
 */

// See KT-6399: exhaustive whens on platform enums

// FILE: J.java

public enum J {
    A, B;

    public static J create() {
        return J.A;
    }
}

// FILE: K.kt

fun foo(): Int {
    when (J.create()) {
        J.A -> return 1
        J.B -> return 2
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, integerLiteral, javaProperty, javaType,
smartcast, whenExpression, whenWithSubject */

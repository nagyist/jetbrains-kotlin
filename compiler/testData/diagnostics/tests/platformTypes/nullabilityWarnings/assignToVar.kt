// RUN_PIPELINE_TILL: FRONTEND
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt

var v: J = J()
var n: J? = J()

fun test() {
    v = J.staticNN
    v = <!TYPE_MISMATCH!>J.staticN<!>
    v = J.staticJ

    n = J.staticNN
    n = J.staticN
    n = J.staticJ
}

/* GENERATED_FIR_TAGS: assignment, flexibleType, functionDeclaration, javaFunction, javaProperty, javaType, nullableType,
propertyDeclaration */

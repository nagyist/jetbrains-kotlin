// JSPECIFY_STATE: strict
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: conflictinglyannotatedpackage/package-info.java

@NullUnmarked
@NullMarked
package conflictinglyannotatedpackage;

import org.jspecify.annotations.*;

// FILE: conflictinglyannotatedpackage/UnannotatedType.java

package conflictinglyannotatedpackage;

public interface UnannotatedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: unannotatedpackage/ConflictinglyAnnotatedType.java

package unannotatedpackage;

import org.jspecify.annotations.*;

@NullUnmarked
@NullMarked
public interface ConflictinglyAnnotatedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: unannotatedpackage/UnannotatedType.java

package unannotatedpackage;

import org.jspecify.annotations.*;

public interface UnannotatedType {
    @NullUnmarked
    @NullMarked
    public String conflictinglyAnnotatedProduce();
    @NullUnmarked
    @NullMarked
    public void conflictinglyAnnotatedConsume(String arg);
}

// FILE: unannotatedpackage/UnannotatedTypeWithConflictinglyAnnotatedConstructor.java

package unannotatedpackage;

import org.jspecify.annotations.*;

public class UnannotatedTypeWithConflictinglyAnnotatedConstructor {
    @NullUnmarked
    @NullMarked
    public UnannotatedTypeWithConflictinglyAnnotatedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: conflictinglyannotatedpackage.UnannotatedType {
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestB: unannotatedpackage.ConflictinglyAnnotatedType {
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestC: unannotatedpackage.UnannotatedType {
    override fun conflictinglyAnnotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

fun test(
    a: conflictinglyannotatedpackage.UnannotatedType,
    b: unannotatedpackage.ConflictinglyAnnotatedType,
    c: unannotatedpackage.UnannotatedType
) {
    a.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    c.conflictinglyAnnotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    unannotatedpackage.UnannotatedTypeWithConflictinglyAnnotatedConstructor(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}

// RUN_PIPELINE_TILL: FRONTEND
// FILE: ExceptionTracker.kt

interface ExceptionTracker : <!EXPOSED_SUPER_INTERFACE!>LockBasedStorageManager.ExceptionHandlingStrategy<!> {
}

// FILE: StorageManager.kt

interface StorageManager : <!CYCLIC_INHERITANCE_HIERARCHY!>ExceptionTracker<!> {
    fun foo()
}

// FILE: LockBasedStorageManager.java

class LockBasedStorageManager extends StorageManager {
    interface ExceptionHandlingStrategy {
        void bar();
    }

    @Override
    void foo() {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, javaType */

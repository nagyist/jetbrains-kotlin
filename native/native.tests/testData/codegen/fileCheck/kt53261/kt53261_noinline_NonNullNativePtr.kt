// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// DISABLE_IR_VISIBILITY_CHECKS: ANY

import kotlinx.cinterop.*

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlin.native.internal.NonNullNativePtr#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlin.native.internal.NonNullNativePtr#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlin.native.internal.NonNullNativePtr#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-OPT: call ptr @"kfun:kotlin.native.internal#<NonNullNativePtr-unbox>(kotlin.Any?){}kotlin.native.internal.NonNullNativePtr?"

// This test is useless in debug mode.
// TODO(KT-59288): add ability to ignore tests in debug mode
// CHECK-DEBUG-LABEL: define ptr @"kfun:#box(){}kotlin.String"

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String = memScoped {
    val var1: IntVar = alloc()
    val var2: IntVar = alloc()
    // The first one is K1, the second one is K2.
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    return if (var1.ptr.value as Any == var2.ptr.value as Any) "FAIL" else "OK"
}

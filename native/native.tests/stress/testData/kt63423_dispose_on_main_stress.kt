/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// KIND: STANDALONE_NO_TR
// Test depends on macOS-specific AppKit
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: targetFamily=IOS
// DISABLE_NATIVE: targetFamily=TVOS
// DISABLE_NATIVE: targetFamily=WATCHOS
// requires some GC and its careful timing
// DISABLE_NATIVE: gcType=NOOP
// DISABLE_NATIVE: gcScheduler=AGGRESSIVE
// KT-65261: TODO
// DISABLE_NATIVE: useThreadStateChecker=ENABLED
// KT-75321: legacy allocators allocates extra ~10MiB during finalization
// DISABLE_NATIVE: alloc=STD

// Do not use mmap for allocations to hopefully better track RSS usage.
// FREE_COMPILER_ARGS: -Xbinary=gcSchedulerType=manual -Xbinary=disableMmap=true -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
headers = objclib.h
linkerOpts = -framework AppKit

// FILE: objclib.h
#include <objc/NSObject.h>

@interface OnDestroyHook : NSObject
- (instancetype)init;
@end

#ifdef __cplusplus
extern "C" {
#endif

void startApp(void (^task)());
BOOL isMainThread();
void spin();

#ifdef __cplusplus
}
#endif

// FILE: objclib.mm
#include "objclib.h"

#include <atomic>
#include <cinttypes>
#include <map>

#include <dispatch/dispatch.h>
#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>

std::map<uintptr_t, std::atomic<bool>> dictionary;

@implementation OnDestroyHook
- (instancetype)init {
    if (self = [super init]) {
        dictionary[(uintptr_t)self].store(true);
    }
    return self;
}

- (void)dealloc {
    dictionary[(uintptr_t)self].store(false);
}

@end

extern "C" void startApp(void (^task)()) {
    dispatch_async(dispatch_get_main_queue(), ^{
        // At this point all other scheduled main queue tasks were already executed.
        // Executing via performBlock to allow a recursive run loop in `spin()`.
        [[NSRunLoop currentRunLoop] performBlock:^{
            task();
            [NSApp terminate:NULL];
        }];
    });
    [[NSApplication sharedApplication] run];
}

extern "C" BOOL isMainThread() {
    return [NSThread isMainThread];
}

extern "C" void spin() {
    if ([NSRunLoop currentRunLoop] != [NSRunLoop mainRunLoop]) {
        fprintf(stderr, "Must spin main run loop\n");
        exit(1);
    }
    while (true) {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
        bool done = true;
        for (auto& kvp : dictionary) {
            if (kvp.second.load()) {
                done = false;
                break;
            }
        }
        if (done) return;
    }
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import objclib.*

import kotlin.native.internal.MemoryUsageInfo
import kotlin.test.*
import kotlinx.cinterop.*

class PeakRSSChecker(private val rssDiffLimitBytes: Long) {
    // On Linux, the child process might immediately commit the same amount of memory as the parent.
    // So, measure difference between peak RSS measurements.
    private val initialBytes = MemoryUsageInfo.peakResidentSetSizeBytes.also {
        check(it != 0L) { "Error trying to obtain peak RSS. Check if current platform is supported" }
    }

    fun check(): Long {
        val diffBytes = MemoryUsageInfo.peakResidentSetSizeBytes - initialBytes
        check(diffBytes <= rssDiffLimitBytes) { "Increased peak RSS by $diffBytes bytes which is more than $rssDiffLimitBytes" }
        println("Increased peak RSS by $diffBytes bytes out of allowed $rssDiffLimitBytes")
        return diffBytes
    }
}

fun alloc(): Unit = autoreleasepool {
    OnDestroyHook()
    Unit
}

fun waitDestruction() {
    assertTrue(isMainThread())
    kotlin.native.runtime.GC.collect()
    spin()
}

fun main() = startApp {
    repeat(500000) {
        alloc()
    }
    val peakRSSChecker = PeakRSSChecker(10_000_000L) // ~10MiB allowed difference for running finalizers
    waitDestruction()
    peakRSSChecker.check()
}
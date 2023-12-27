package KotlinNative

import KotlinNative.buildTypes.*
import KotlinNative.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("KotlinNative")
    name = "Kotlin/Native"

    vcsRoot(Kotlin_Native_IOS_Upload_Test)
    vcsRoot(Kotlinx_Coroutines)

    buildType(KotlinNativeDist_macos_x64_BUNDLE)
    buildType(KotlinNativeDist_linux_x64_DIST)
    buildType(KotlinNativeDist_mingw_x64_DIST)
    buildType(KotlinNativeMacSanityRemoteRunComposite)
    buildType(KotlinNativeDist_mingw_x64_BUNDLE)
    buildType(KotlinNativeKlibTests)
    buildType(KotlinNativePerformanceTests_1)
    buildType(KotlinNativeRuntimePreCommitTests)
    buildType(KotlinNativePreCommitPerformanceComposite)
    buildType(KotlinNativeDist_macos_arm64_BUNDLE)
    buildType(KotlinNativeLatestStableXcodeComposite)
    buildType(KotlinNativeWeeklyComposite)
    buildType(KotlinNativeNightlyComposite)
    buildType(KotlinNativeLatestBetaXcodeComposite)
    buildType(KotlinNativePerformancePreCommitTests)
    buildType(KotlinNativeRuntimeTests)
    buildType(KotlinNativeGradleSamples)
    buildType(KotlinNativeDist_macos_x64_DIST)
    buildType(KotlinNativeDist_macos_arm64_DIST)
    buildType(KotlinNativeDist_linux_x64_BUNDLE)
    buildType(KotlinNativePreCommitComposite)

    subProject(XcodeUpdate.Project)
    subProject(KotlinNativeLatestXcodeTests.Project)
    subProject(KotlinNativePerformanceTests.Project)
    subProject(KotlinNativeTests.Project)
})

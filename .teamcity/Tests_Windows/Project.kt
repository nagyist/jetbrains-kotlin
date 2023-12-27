package Tests_Windows

import Tests_Windows.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Tests_Windows")
    name = "Tests (Windows)"

    buildType(GradleIntegrationTestsGradleandKotlindaemonsKGPtests_WINDOWS)
    buildType(GradleIntegrationTestsAndroidKGPtests_WINDOWS)
    buildType(JSCompilerTestsIR_WINDOWS)
    buildType(KAPTCompilerTests_WINDOWS)
    buildType(WASMCompilerTestsK2_WINDOWS)
    buildType(WASMCompilerTests_WINDOWS)
    buildType(JVMCompilerTests_WINDOWS)
    buildType(MiscCompilerTests_WINDOWS)
    buildType(JSCompilerTestsIRES6_WINDOWS)
    buildType(JsTestsNightlyWindowsAggregate)
    buildType(JSCompilerTestsFIR_WINDOWS)
    buildType(GradleIntegrationTestsGradleKotlinMPPtests_WINDOWS)
    buildType(JSCompilerTests_WINDOWS)
    buildType(GradleIntegrationTestsJVM_WINDOWS)
    buildType(JSCompilerTestsFIRES6_WINDOWS)
    buildType(GenerateTests_WINDOWS)
    buildType(GradleIntegrationTestsOtherGradleKotlintests_WINDOWS)
    buildType(GradleIntegrationTestsGradleKotlinJStests_WINDOWS)
    buildType(ParcelizeTests_WINDOWS)
    buildType(GradleIntegrationTestsNightlyAggregate)
    buildType(GradleIntegrationTestsGradleKotlinnativetests_WINDOWS)
})

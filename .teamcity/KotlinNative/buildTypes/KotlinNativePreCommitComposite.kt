package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object KotlinNativePreCommitComposite : BuildType({
    name = "Aggregate Native pre-commit builds (rrn/*)"
    description = "Pre-commit testing"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("env.KONAN_DATA_DIR", "%system.agent.persistent.cache%/konan")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot)

        showDependenciesChanges = true
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            branchFilter = """
                +:rrn/*
                -:rrn/perf/*
            """.trimIndent()
        }
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-native-build-notifications"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            firstFailureAfterSuccess = true
            firstSuccessAfterFailure = true
        }
    }

    dependencies {
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_DIST_SANITY_debug_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_DIST_SANITY_debug_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_DIST_SANITY_opt_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_DIST_SANITY_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_arm64_DIST_SANITY_debug_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_arm64_DIST_SANITY_debug_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_arm64_DIST_SANITY_opt_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_arm64_DIST_SANITY_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_DIST_SANITY_debug_k1_enabled_aggressive_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_DIST_SANITY_debug_k1_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_DIST_SANITY_debug_k2_enabled_aggressive_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_DIST_SANITY_debug_k2_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_DIST_SANITY_opt_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_DIST_SANITY_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_DIST_SANITY_debug_k1_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_DIST_SANITY_debug_k2_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_DIST_SANITY_opt_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_DIST_SANITY_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_DIST_SANITY_debug_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_DIST_SANITY_debug_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_DIST_SANITY_opt_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_DIST_SANITY_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativeRuntimePreCommitTests) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_With_PL_linux_x64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_With_PL_linux_x64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_With_PL_macos_arm64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_With_PL_mingw_x64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_Without_PL_linux_x64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_Without_PL_linux_x64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_Without_PL_macos_arm64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_Without_PL_mingw_x64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_With_PL_linux_arm64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_With_PL_linux_x64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_With_PL_mingw_x64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_ios_simulator_arm64_dist_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_ios_simulator_arm64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_ios_simulator_arm64_dist_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_arm64_dist_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_arm64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_arm64_dist_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_x64_dist_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_x64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_x64_dist_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_dist_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_dist_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_dist_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }
})

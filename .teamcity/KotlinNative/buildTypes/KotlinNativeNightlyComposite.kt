package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications

object KotlinNativeNightlyComposite : BuildType({
    name = "Aggregate Native nightly builds (master)"
    description = "Nightly testing"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "%globalGradleParameters%")
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
            buildFailed = true
            buildFinishedSuccessfully = true
        }
    }

    dependencies {
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_arm64_BUNDLE_RUN_opt_k1_none_disabled_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_arm64_BUNDLE_RUN_opt_k2_none_enabled_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_debug_k1_static_only_dist_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_x64_BUNDLE_RUN_debug_enabled_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_x64_BUNDLE_RUN_debug_enabled_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_x64_BUNDLE_RUN_debug_k1_static_only_dist_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_x64_BUNDLE_RUN_debug_k2_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_x64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_ios_x64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_arm64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_arm64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_BUNDLE_RUN_debug_mimalloc_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_BUNDLE_RUN_debug_mimalloc_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_BUNDLE_RUN_k2_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_linux_x64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_cms_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_cms_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_enabled_aggressive_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_enabled_aggressive_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_enabled_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_enabled_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_k1_static_only_dist_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_mimalloc_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_mimalloc_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_noop_enabled_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_noop_enabled_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_stms_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_stms_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_k2_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_opt_aggressive_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_opt_aggressive_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_opt_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_opt_stms_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_opt_stms_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_aggressive_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_aggressive_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_enabled_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_enabled_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_k1_static_only_dist_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_k2_static_only_dist_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_mimalloc_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_mimalloc_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_noop_enabled_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_debug_noop_enabled_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_opt_aggressive_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_opt_aggressive_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_opt_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_opt_stms_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_macos_x64_BUNDLE_RUN_opt_stms_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_BUNDLE_RUN_debug_mimalloc_k1_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_BUNDLE_RUN_debug_mimalloc_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_BUNDLE_RUN_k2_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_mingw_x64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_opt_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_x64_BUNDLE_RUN_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_x64_BUNDLE_RUN_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_x64_BUNDLE_RUN_opt_k1_none_disabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.BackendNativeTest_watchos_x64_BUNDLE_RUN_opt_k2_none_enabled) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativeTests.buildTypes.CoroutinesBinaryCompatibilityTests_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.CoroutinesBinaryCompatibilityTests_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeGradleSamples) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeKlibTests) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests_1) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(Deploy.buildTypes.KotlinNativePublish) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeRuntimeTests) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.KotlinNativeiOS_Upload_Test_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2OneStageAndK1_linux_x64_bundle_mode_1_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2OneStageAndK1_macos_arm64_bundle_mode_1_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2OneStage_mingw_x64_bundle_mode_1_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_Only_PL_macos_x64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_Only_PL_macos_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_Only_PL_macos_x64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_linux_x64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_linux_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_linux_x64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_mingw_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_K2_mingw_x64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Only_PL_macos_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_e_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_Only_PL_macos_x64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_Only_PL_macos_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_Only_PL_macos_x64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_Without_PL_linux_x64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_x64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_linux_x64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_mingw_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_Without_K2_mingw_x64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_linux_x64_bundle_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_linux_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerTest_mingw_x64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        artifacts(KotlinNativeDist_linux_x64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-linux-x86_64-*.tar.gz=>bundle"
        }
        artifacts(KotlinNativeDist_macos_arm64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-macos-aarch64-*.tar.gz=>bundle"
        }
        artifacts(KotlinNativeDist_macos_x64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-macos-x86_64-*.tar.gz=>bundle"
        }
        artifacts(KotlinNativeDist_mingw_x64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-windows-x86_64-*.zip=>bundle"
        }
    }
})

package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object KotlinNativeLatestBetaXcodeComposite : BuildType({
    name = "Latest Beta Xcode tests (Native, composite)"
    description = "Xcode latest beta tests run on VM agents"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("reverse.dep.*.env.KONAN_USE_INTERNAL_SERVER", "0")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("reverse.dep.Kotlin_BuildPlayground_Titan_KotlinNativeDist*.env.KONAN_USE_INTERNAL_SERVER", "1")
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
        schedule {
            enabled = false
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Saturday
                hour = 0
            }
            branchFilter = """
                +:<default>
                -:*
            """.trimIndent()
            triggerBuild = always()
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
            buildFailed = true
            buildFinishedSuccessfully = true
        }
    }

    dependencies {
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_ios_arm64_BUNDLE_RUN_debug_k2_static_everywhere_enabled_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_cms_disabled_debug_disabled_k2_static_only_dist_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_disabled_debug_disabled_k1_static_only_dist_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_noop_aggressive_mimalloc_disabled_no_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_stms_aggressive_enabled_debug_disabled_k1_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_stms_aggressive_mimalloc_disabled_opt_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_cms_aggressive_mimalloc_enabled_debug_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_cms_mimalloc_disabled_no_disabled_k1_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_disabled_debug_disabled_k1_static_only_dist_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_mimalloc_disabled_opt_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_noop_disabled_debug_disabled_k1_static_only_dist_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_macos_arm64_BUNDLE_RUN_stms_disabled_debug_disabled_k2_static_only_dist_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_tvos_arm64_BUNDLE_RUN_stms_aggressive_mimalloc_disabled_opt_k2_none_enabled_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_aggressive_mimalloc_disabled_no_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_cms_aggressive_disabled_opt_disabled_k1_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_noop_mimalloc_enabled_debug_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_stms_aggressive_disabled_no_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_watchos_arm64_BUNDLE_RUN_cms_enabled_debug_k1_none_enabled_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_cms_aggressive_disabled_no_disabled_k1_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_enabled_debug_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_noop_mimalloc_disabled_opt_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_stms_mimalloc_disabled_opt_disabled_k2_none_enabled_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeKlib_Tests_macos_arm64_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeRuntime_Tests_macos_arm64_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeSamples_macos_arm64_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeiOS_Upload_Test_macos_arm64_beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_e_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_e_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }
})

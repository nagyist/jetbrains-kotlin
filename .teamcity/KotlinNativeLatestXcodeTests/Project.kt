package KotlinNativeLatestXcodeTests

import KotlinNativeLatestXcodeTests.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("KotlinNativeLatestXcodeTests")
    name = "Latest Xcode Tests"

    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_stms_aggressive_mimalloc_disabled_opt_disabled_k2_none_enabled_beta)
    buildType(KotlinNativeRuntime_Tests_macos_arm64_stable)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_cms_aggressive_disabled_no_disabled_k1_none_enabled_stable)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_enabled_debug_disabled_k2_none_enabled_stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_stms_disabled_debug_disabled_k2_static_only_dist_enabled_stable)
    buildType(KotlinNativeDist_macos_arm64_DIST_stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_cms_mimalloc_disabled_no_disabled_k1_none_enabled_beta)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_disabled_debug_disabled_k1_static_only_dist_enabled_beta)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_noop_aggressive_mimalloc_disabled_no_disabled_k2_none_enabled_beta)
    buildType(BackendNativeTest_tvos_arm64_BUNDLE_RUN_stms_aggressive_mimalloc_disabled_opt_k2_none_enabled_enabled_beta)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_cms_aggressive_mimalloc_enabled_debug_disabled_k2_none_enabled_stable)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(KotlinNativeKlib_Tests_macos_arm64_stable)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_e_target__stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_noop_disabled_debug_disabled_k1_static_only_dist_enabled_stable)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_cms_aggressive_disabled_no_disabled_k1_none_enabled_beta)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled_beta)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_stms_aggressive_disabled_no_disabled_k2_none_enabled_stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled_beta)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_cms_aggressive_disabled_opt_disabled_k1_none_enabled_stable)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_noop_mimalloc_disabled_opt_disabled_k2_none_enabled_stable)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_stms_aggressive_enabled_debug_disabled_k1_none_enabled_beta)
    buildType(KotlinNativeDist_macos_arm64_BUNDLE_beta)
    buildType(NativeCompilerTest_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target__beta)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_stms_disabled_debug_disabled_k2_static_only_dist_enabled_beta)
    buildType(NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_noop_aggressive_mimalloc_disabled_no_disabled_k2_none_enabled_stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_disabled_debug_disabled_k1_static_only_dist_enabled_stable)
    buildType(KotlinNativeRuntime_Tests_macos_arm64_beta)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_e_target__beta)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_aggressive_mimalloc_disabled_no_disabled_k2_none_enabled_stable)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target__stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled_stable)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_e_target__stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_cms_aggressive_mimalloc_enabled_debug_disabled_k2_none_enabled_beta)
    buildType(KotlinNativeDist_macos_arm64_BUNDLE_stable)
    buildType(KotlinNativeiOS_Upload_Test_macos_arm64_beta)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_mimalloc_disabled_opt_disabled_k2_none_enabled_stable)
    buildType(KotlinNativeKlib_Tests_macos_arm64_beta)
    buildType(KotlinNativeDist_macos_arm64_DIST_beta)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target__beta)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_cms_aggressive_disabled_opt_disabled_k1_none_enabled_beta)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(BackendNativeTest_watchos_arm64_BUNDLE_RUN_cms_enabled_debug_k1_none_enabled_enabled_stable)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_noop_disabled_debug_disabled_k1_static_only_dist_enabled_beta)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_stms_aggressive_disabled_no_disabled_k2_none_enabled_beta)
    buildType(KotlinNativeSamples_macos_arm64_stable)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_disabled_debug_disabled_k1_static_only_dist_enabled_beta)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_cms_mimalloc_disabled_no_disabled_k1_none_enabled_stable)
    buildType(NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_disabled_debug_disabled_k1_static_only_dist_enabled_stable)
    buildType(BackendNativeTest_ios_arm64_BUNDLE_RUN_debug_k2_static_everywhere_enabled_enabled_beta)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_stms_aggressive_mimalloc_disabled_opt_disabled_k2_none_enabled_stable)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_noop_mimalloc_enabled_debug_disabled_k2_none_enabled_stable)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_enabled_debug_disabled_k2_none_enabled_beta)
    buildType(BackendNativeTest_ios_arm64_BUNDLE_RUN_debug_k2_static_everywhere_enabled_enabled_stable)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_debug_k2_static_only_dist_enabled_stable)
    buildType(KotlinNativeSamples_macos_arm64_beta)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_cms_disabled_debug_disabled_k2_static_only_dist_enabled_stable)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_cms_disabled_debug_disabled_k2_static_only_dist_enabled_beta)
    buildType(BackendNativeTest_ios_simulator_arm64_BUNDLE_RUN_stms_aggressive_enabled_debug_disabled_k1_none_enabled_stable)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_e_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_noop_mimalloc_disabled_opt_disabled_k2_none_enabled_beta)
    buildType(BackendNativeTest_macos_arm64_BUNDLE_RUN_mimalloc_disabled_opt_disabled_k2_none_enabled_beta)
    buildType(NativeCompilerTest_Without_K2_macos_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_stms_mimalloc_disabled_opt_disabled_k2_none_enabled_beta)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_m_useThreadStateChecker_d_target__stable)
    buildType(BackendNativeTest_watchos_simulator_arm64_BUNDLE_RUN_stms_mimalloc_disabled_opt_disabled_k2_none_enabled_stable)
    buildType(NativeCompilerTest_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_noop_mimalloc_enabled_debug_disabled_k2_none_enabled_beta)
    buildType(NativeCompilerTest_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_f_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(NativeCompilerTest_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__stable)
    buildType(KotlinNativeiOS_Upload_Test_macos_arm64_stable)
    buildType(NativeCompilerTest_K2_macos_arm64_bundle_mode_2_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target__beta)
    buildType(BackendNativeTest_tvos_simulator_arm64_BUNDLE_RUN_aggressive_mimalloc_disabled_no_disabled_k2_none_enabled_beta)
    buildType(BackendNativeTest_tvos_arm64_BUNDLE_RUN_stms_aggressive_mimalloc_disabled_opt_k2_none_enabled_enabled_stable)
    buildType(NativeCompilerTest_Without_K2_ios_simulator_arm64_bundle_mode_2_optimizationMode_d_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_e_target__beta)
    buildType(BackendNativeTest_watchos_arm64_BUNDLE_RUN_cms_enabled_debug_k1_none_enabled_enabled_beta)
})

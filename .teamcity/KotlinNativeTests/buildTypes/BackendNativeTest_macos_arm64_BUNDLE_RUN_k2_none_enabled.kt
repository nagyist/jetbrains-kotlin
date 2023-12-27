package KotlinNativeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object BackendNativeTest_macos_arm64_BUNDLE_RUN_k2_none_enabled : BuildType({
    name = "🍏ᵐ Target-Specific Tests (for bundle) K2/two-stage (Native, Macos aarch64)"
    description = "Tests from the :kotlin-native:backend.native:tests project"

    artifactRules = """
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("kotlin.native.test_compile_only", "")
        param("gradleParameters", "--info --full-stacktrace %globalGradleBuildScanParameters% -Pkotlin.build.testRetry.maxRetries=0 -Pkotlin.build.isObsoleteJdkOverrideEnabled=true")
        param("kotlin.native.test_target", "macos_arm64")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%/test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("env.KONAN_DATA_DIR", "%system.agent.persistent.cache%/konan")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        param("kotlin.native.test_cache", "")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_1_6", "%env.JDK_1_8%")
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_17_0%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_9_0", "%env.JDK_11_0%")
        param("kotlin.native.test_two_stage", "-Ptest_two_stage")
        param("env.JDK_1_7", "%env.JDK_1_8%")
        param("kotlin.native.test_flags", "")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        param("kotlin.native.target_opts", "")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Set up Git"
            scriptContent = """
                "%env.TEAMCITY_GIT_PATH%" -C "%teamcity.build.checkoutDir%/kotlin" config user.email teamcity-demo-noreply@jetbrains.com
                "%env.TEAMCITY_GIT_PATH%" -C "%teamcity.build.checkoutDir%/kotlin" config user.name TeamCity
            """.trimIndent()
        }
        script {
            name = "Print KONAN_USE_INTERNAL_SERVER value"
            scriptContent = "printenv | grep KONAN_USE_INTERNAL_SERVER || true"
        }
        script {
            name = "Print current Xcode version"
            scriptContent = "xcode-select -p"
        }
        gradle {
            name = "All backend tests (:kotlin-native:backend.native:tests:run task)"
            tasks = ":kotlin-native:backend.native:tests:run"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --no-parallel                 --continue
                                -Pkotlin.incremental=false
                                -Pkonan.home=%kotlin.native.test_dist%
                                -Pkotlin.native.home=%kotlin.native.test_dist%
                                -Ptest_target=%kotlin.native.test_target% 
                                "-Ptest_flags=%kotlin.native.test_flags%"
                                -Pkotlin.native.enabled=true
                                -Pkotlin.native.allowRunningCinteropInProcess=true
                                %kotlin.native.test_cache%
                                %kotlin.native.test_two_stage%
                                %kotlin.native.test_compile_only%
                                %kotlin.native.target_opts%
                                -Pbuild.number=%kotlin.native.version%
                -PkonanMetaVersion=%konanMetaVersion%
                -PkonanVersion=%konanVersion%
            """.trimIndent()
            enableStacktrace = true
            jdkHome = "%env.JDK_17_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 240
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz!kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-beta-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        noLessThanVer("tools.xcode.version", "15.1")
        noLessThanVer("tools.xcode.version", "13.0")
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-beta-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        noLessThanVer("tools.xcode.version", "15.1")
    }
})

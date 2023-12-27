package KotlinNativePerformanceTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativeSpace_iOS_Performance_ios_arm64 : BuildType({
    name = "🍎 Space iOS Performance Test (Native, iOS ARM64)"
    description = "Build train of kotlinx.atomicfu and kotlinx.coroutines used to measure and evaluate compiler performance changes"

    artifactRules = """
        %kotlin.native.artifacts.logs%
        kotlin/kotlin-native/performance/build/externalReport.json
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("env.BUILD_TYPE", "DEV")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlinGradleParameters", "%globalGradleParameters%")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        password("system.spaceUsername", "credentialsJSON:5c393b0b-c01a-47c4-ad54-2f3cb5d8eabe")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("env.KONAN_DATA_DIR", "%system.agent.persistent.cache%/konan")
        param("env.nativeKotlinRepo", "")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        param("env.JDK_1_6", "%env.JDK_1_8%")
        password("system.spacePassword", "credentialsJSON:c445e8ac-a6c5-464e-b8a5-2d7f99d4a416")
        param("env.JDK_9_0", "%env.JDK_11_0%")
        param("env.JDK_1_7", "%env.JDK_1_8%")
        param("kotlin.native.target_opts", "")
        param("gradleParameters", "--info --full-stacktrace %globalGradleBuildScanParameters% -Pkotlin.build.testRetry.maxRetries=0 -Pkotlin.build.isObsoleteJdkOverrideEnabled=true")
        password("system.artifactory.apikey", "credentialsJSON:edd14ef2-4f23-4527-b823-1d4407091e2c")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("env.nativeKotlinVersion", "")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_17_0%", display = ParameterDisplay.HIDDEN)
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk11", "%env.JDK_11_0%", display = ParameterDisplay.HIDDEN)
        param("system.kotlin_compiler_repo", "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(KotlinNativePerformanceTests.vcsRoots.SpaceVCS, "+:. => space", "-:docker", "-:deployments")

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
            name = "Publish compiler and plugin to maven local"
            tasks = "clean install"
            buildFile = "build.gradle.kts"
            workingDir = "kotlin"
            gradleParams = "%gradleParameters% --parallel %kotlinGradleParameters% -Pbuild.number=%kotlin.native.version%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Space framework build"
            tasks = ":app:app-ios-native:copyFramework"
            buildFile = "build.gradle"
            workingDir = "space"
            gradleParams = """
                %gradleParameters% --parallel -Pkotlin.build.type=DEBUG
                -PincludeIosModules=true
                -Pkotlin.native.home=%kotlin.native.test_dist%
                -PkotlinCompilerRepo=%system.kotlin_compiler_repo%
                -Psnapshot_kotlin_version=%kotlin.native.version%
                --stacktrace
                -PuseCacheRedirector
                --no-configuration-cache
            """.trimIndent()
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Copy results"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            scriptContent = """
                mkdir -p kotlin/kotlin-native/performance/build
                cp space/app/app-ios-native/build/perfReports/iosPerfReportDebugFramework.txt \
                kotlin/kotlin-native/performance/build/spaceReport.txt
                ls kotlin/kotlin-native/performance/build
            """.trimIndent()
        }
        gradle {
            name = "Register results"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = "registerExternalBenchmarks"
            buildFile = "build.gradle"
            workingDir = "kotlin/kotlin-native/performance"
            gradleParams = """
                %gradleParameters% --parallel -Pkotlin.native.home=%kotlin.native.test_dist%
                -PexternalReports=spaceReport.txt
                -PexternalBenchmarksReport=spaceFrameworkReport.json
                -PkotlinVersion=%kotlin.native.version%
                -Pkotlin.native.performance.server.url=%kotlin.native.performance.server.url%
            """.trimIndent()
            gradleWrapperPath = "../.."
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
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
        contains("teamcity.agent.name", "kotlin-macos-m1-munit620")
    }
})

package KotlinNativePerformanceTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativeLibraries_Compilation_Performance_linux_x64 : BuildType({
    name = "🐧 Libraries Compilation Performance (Native, Linux x86_64)"
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
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("env.KONAN_DATA_DIR", "%system.agent.persistent.cache%/konan")
        param("env.nativeKotlinRepo", "")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        param("system.atomicfu_version", "%system.DeployVersion%")
        param("system.DeployVersion", "0.0.1-train-%build.counter%")
        param("kotlin.native.target_opts", "")
        param("system.build_snapshot_train", "true")
        param("system.skip_snapshot_checks", "true")
        param("gradleParameters", "--info --full-stacktrace %globalGradleBuildScanParameters% %globalGradleCacheNodeParameters% -Pkotlin.build.testRetry.maxRetries=0")
        password("system.artifactory.apikey", "credentialsJSON:edd14ef2-4f23-4527-b823-1d4407091e2c")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("env.nativeKotlinVersion", "")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk11", "%env.JDK_11_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(KotlinNativePerformanceTests.vcsRoots.Kotlinx_Atomicfu, "+:. => kotlinx.atomicfu")
        root(KotlinNative.vcsRoots.Kotlinx_Coroutines, "+:. => kotlinx.coroutines")

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
            name = "Atomicfu publication"
            tasks = "clean publishToMavenLocal"
            buildFile = "build.gradle"
            workingDir = "kotlinx.atomicfu"
            gradleParams = """
                %gradleParameters% --parallel %kotlinGradleParameters%
                -Pkotlin.native.home=%kotlin.native.test_dist%
                -Pkotlin_snapshot_version=%kotlin.native.version%
                -Pbuild_snapshot_train=true
                --stacktrace
            """.trimIndent()
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Coroutines test binary"
            tasks = "clean :kotlinx-coroutines-core:linuxX64TestBinaries"
            buildFile = "build.gradle"
            workingDir = "kotlinx.coroutines"
            gradleParams = """
                %gradleParameters% --parallel -Pkotlin.native.home=%kotlin.native.test_dist% 
                -Pkotlin_snapshot_version=%kotlin.native.version%
                -Pbuild_snapshot_train=true
                --stacktrace
            """.trimIndent()
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Copy results"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            scriptContent = """
                mkdir -p kotlin/kotlin-native/performance/build
                cp kotlinx.coroutines/kotlinx-coroutines-core/build/perfReports/linuxX64PerfReportDebugTest.txt \
                kotlin/kotlin-native/performance/build/coroutinesReport.txt
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
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz!kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
            artifacts {
                artifactRules = "gradle.properties"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("teamcity.agent.name", "kotlin-linux-x64-metal-munit787")
    }
})

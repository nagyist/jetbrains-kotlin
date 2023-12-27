package KotlinNativeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object NativeCompilerTest_Without_K2_linux_x64_dist_mode_1_optimizationMode_d_cacheMode_d_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_ : BuildType({
    name = "🐧 Compiler Tests Without K2 (for dist) one-stage/opt.debug/cache.dist (Native, Linux x86_64)"
    description = "Tests from the :native:native.tests project"

    artifactRules = """
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "--info --full-stacktrace %globalGradleBuildScanParameters% %globalGradleCacheNodeParameters% -Pkotlin.build.testRetry.maxRetries=0")
        param("kotlin.native.test_target", "linux_x64")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%/test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("env.KONAN_DATA_DIR", "%system.agent.persistent.cache%/konan")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
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
        gradle {
            name = "Compile everything before running tests"
            tasks = ":native:native.tests:test"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel --continue
                -Pkotlin.native.enabled=true
                -Pkotlin.internal.native.test.nativeHome=%kotlin.native.test_dist%
                -Pkotlin.incremental=false
                -Pkotlin.internal.native.test.mode=ONE_STAGE_MULTI_MODULE
                -Pkotlin.internal.native.test.optimizationMode=DEBUG
                -Pkotlin.internal.native.test.cacheMode=STATIC_ONLY_DIST
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                -PkonanMetaVersion=%konanMetaVersion%
                -PkonanVersion=%konanVersion%
                "-Pkotlin.native.tests.tags=!frontend-fir"
                -Pkotlin.build.disable.verification.tasks=true
            """.trimIndent()
            enableStacktrace = true
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Native compiler tests"
            tasks = ":native:native.tests:test"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel --continue
                -Pkotlin.native.enabled=true
                -Pkotlin.internal.native.test.nativeHome=%kotlin.native.test_dist%
                -Pkotlin.incremental=false
                -Pkotlin.internal.native.test.mode=ONE_STAGE_MULTI_MODULE
                -Pkotlin.internal.native.test.optimizationMode=DEBUG
                -Pkotlin.internal.native.test.cacheMode=STATIC_ONLY_DIST
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                -PkonanMetaVersion=%konanMetaVersion%
                -PkonanVersion=%konanVersion%
                "-Pkotlin.native.tests.tags=!frontend-fir"
            """.trimIndent()
            enableStacktrace = true
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
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_DIST) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-linux-x86_64-%kotlin.native.version%.tar.gz!kotlin-native-linux-x86_64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-beta-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-beta-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

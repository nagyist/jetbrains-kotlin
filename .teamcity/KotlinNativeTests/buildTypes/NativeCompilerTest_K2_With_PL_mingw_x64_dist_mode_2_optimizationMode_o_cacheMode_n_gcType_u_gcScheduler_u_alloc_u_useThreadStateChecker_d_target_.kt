package KotlinNativeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object NativeCompilerTest_K2_With_PL_mingw_x64_dist_mode_2_optimizationMode_o_cacheMode_n_gcType_u_gcScheduler_u_alloc_u_useThreadStateChecker_d_target_ : BuildType({
    name = "🪟 Compiler Tests K2 With PL (for dist) opt.opt/cache.no (Native, Windows x86_64)"
    description = "Tests from the :native:native.tests project"

    artifactRules = """
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "--info --full-stacktrace %globalGradleBuildScanParameters% %globalGradleCacheNodeParameters% -Pkotlin.build.testRetry.maxRetries=0")
        param("kotlin.native.test_target", "mingw_x64")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%/test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("env.KONAN_DATA_DIR", "%system.agent.persistent.cache%/konan")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("teamcity.internal.gradle.runner.launch.mode", "gradle")
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
                -Pkotlin.internal.native.test.mode=TWO_STAGE_MULTI_MODULE
                -Pkotlin.internal.native.test.optimizationMode=OPT
                -Pkotlin.internal.native.test.cacheMode=NO
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                -PkonanMetaVersion=%konanMetaVersion%
                -PkonanVersion=%konanVersion%
                "-Pkotlin.native.tests.tags=^^^!frontend-fir^^^&^^^!no-partial-linkage-may-be-skipped"
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
                -Pkotlin.internal.native.test.mode=TWO_STAGE_MULTI_MODULE
                -Pkotlin.internal.native.test.optimizationMode=OPT
                -Pkotlin.internal.native.test.cacheMode=NO
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                -PkonanMetaVersion=%konanMetaVersion%
                -PkonanVersion=%konanVersion%
                "-Pkotlin.native.tests.tags=^^^!frontend-fir^^^&^^^!no-partial-linkage-may-be-skipped"
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
        dependency(KotlinNative.buildTypes.KotlinNativeDist_mingw_x64_DIST) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-windows-x86_64-%kotlin.native.version%.zip!kotlin-native-windows-x86_64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-beta-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        moreThan("teamcity.agent.hardware.memorySizeMb", "8100")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("teamcity.agent.jvm.os.name", "Windows")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-beta-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        moreThan("teamcity.agent.hardware.memorySizeMb", "8100")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

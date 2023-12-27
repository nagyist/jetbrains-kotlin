package KotlinNativeLatestXcodeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.XmlReport
import jetbrains.buildServer.configs.kotlin.buildFeatures.xmlReport
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativeRuntime_Tests_macos_arm64_stable : BuildType({
    name = "🍏ᵐ [Xcode Stable] Runtime Tests (Native, Macos aarch64)"
    description = "Native runtime (GTest) tests"

    artifactRules = "+:kotlin/kotlin-native/runtime/build/testReports"
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "--info --full-stacktrace %globalGradleBuildScanParameters% -Pkotlin.build.testRetry.maxRetries=0 -Pkotlin.build.isObsoleteJdkOverrideEnabled=true -PcheckXcodeVersion=false")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("env.KONAN_DATA_DIR", "%system.agent.persistent.cache%/konan")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_1_6", "%env.JDK_1_8%")
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_17_0%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_9_0", "%env.JDK_11_0%")
        param("env.JDK_1_7", "%env.JDK_1_8%")
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
            name = "Runtime tests"
            tasks = ":kotlin-native:runtime:hostRuntimeTests"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel -Pbuild.number=%kotlin.native.version%
                -PkonanMetaVersion=%konanMetaVersion%
                -PkonanVersion=%konanVersion% -Pkotlin.native.enabled=true -Pkotlin.native.isNativeRuntimeDebugInfoEnabled=true
            """.trimIndent()
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 240
    }

    features {
        xmlReport {
            reportType = XmlReport.XmlReportType.GOOGLE_TEST
            rules = "kotlin/kotlin-native/runtime/build/testReports/**/report-with-prefixes.xml"
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        noLessThanVer("tools.xcode.version", "13.0")
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        startsWith("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        noLessThanVer("tools.xcode.version", "15.1")
    }
})

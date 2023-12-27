package KotlinNativeLatestXcodeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativeDist_macos_arm64_DIST_stable : BuildType({
    name = "🍏ᵐ [Xcode Stable] Compiler Dist: no platform libs (Native, Macos aarch64)"
    description = "Build configuration"

    artifactRules = """
        kotlin/kotlin-native/kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz
        kotlin/kotlin-native/build/kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
        kotlin/kotlin-native/build/spdx/regular/kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json
        kotlin/kotlin-native/build/spdx/prebuilt/kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "--info --full-stacktrace %globalGradleBuildScanParameters% -Pkotlin.build.isObsoleteJdkOverrideEnabled=true")
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
        param("system.deployVersion", "%kotlin.native.version%")
        text("requirement.jdk17", "%env.JDK_17_0%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_9_0", "%env.JDK_11_0%")
        param("env.JDK_1_7", "%env.JDK_1_8%")
        param("konanMetaVersion", "%build.number.native.meta.version%")
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
            name = "Build Compiler Dist: no platform libs"
            tasks = ":kotlin-native:bundleRegular"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel                 -Pbuild.number=%kotlin.native.version%
                -PkonanMetaVersion=%konanMetaVersion%
                -PkonanVersion=%konanVersion%
                                -Pkotlin.native.enabled=true
                                -Pkotlin.native.allowRunningCinteropInProcess=true
                                -Pkotlin.incremental=false
            """.trimIndent()
            enableStacktrace = true
            jdkHome = "%env.JDK_17_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 200
    }

    features {
        freeDiskSpace {
            requiredSpace = "32gb"
            failBuild = true
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        startsWith("teamcity.agent.name", "titan-kotlin-k8s-native-xcode-stable-macos")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        noLessThanVer("tools.xcode.version", "15.1")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

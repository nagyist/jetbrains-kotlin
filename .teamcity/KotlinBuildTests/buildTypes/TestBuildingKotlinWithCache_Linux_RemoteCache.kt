package KotlinBuildTests.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object TestBuildingKotlinWithCache_Linux_RemoteCache : BuildType({
    name = "🐧 Test building Kotlin with REMOTE cache (Linux)"

    artifactRules = "**/build/libs/*.jar => build-libs.zip"
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", """%globalGradleParameters% -Pkotlin.build.cache.local.enabled=true -Pkotlin.build.cache.local.directory=%teamcity.build.checkoutDir%/build-cache -Pkotlin.build.cache.check.enabled=true "-Dscan.value.Configuration=Test building Kotlin with REMOTE cache (Linux)"""")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("teamcity.internal.gradle.runner.launch.mode", "gradle-tooling-api")
        text("env.GRADLE_ENTERPRISE_ACCESS_KEY", "ge.jetbrains.com=%gradle.enterprise.access.key%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        param("system.deployVersion", "%build.number%")
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("globalGradleParameters", """-Pteamcity=true --info --full-stacktrace "-Pbuild.number=%build.number%" %globalGradleCacheNodeParameters% %globalGradleBuildScanParameters%""")
        param("build.number.default", "${BuildKotlinToDirectoryCache.depParamRefs.buildNumber}-%build.counter%")
        param("globalGradleCacheNodeParameters", " -Pkotlin.build.cache.url=https://gradle-cache.kotlin.intellij.net/cache/ -Pkotlin.build.cache.user=%kotlin.build.cache.user% -Pkotlin.build.cache.password=%kotlin.build.cache.password% -Pkotlin.build.cache.push=true")
        param("globalGradleBuildScanParameters", "-Pkotlin.build.scan.url=%gradle.enterprise.url% -Dscan.tag.kotlin-build-test")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin-in-different-directory")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Build with cache"
            tasks = "dist"
            buildFile = "build.gradle.kts"
            workingDir = "kotlin-in-different-directory"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-bots"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            firstFailureAfterSuccess = true
        }
    }

    dependencies {
        snapshot(BuildKotlinToDirectoryCache) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "aws")
        startsWith("system.cloud.profile_id", "titan")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

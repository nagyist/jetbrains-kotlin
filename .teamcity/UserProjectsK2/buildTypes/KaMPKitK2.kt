package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KaMPKitK2 : BuildType({
    name = "🍏ᵐ [Project] KaMPKit with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /Users/admin/buildAgent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}")
        param("env.ANDROID_HOME", "%env.HOME%/Library/Android/sdk")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.KaMPKitK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "configure gradle"
            workingDir = "user-project"
            scriptContent = """
                echo "org.gradle.java.home=%env.JDK_17_0%" >> gradle.properties
                echo "kotlin_version=${BuildNumber.depParamRefs["deployVersion"]}" >> gradle.properties
                echo "kotlin_repo_url=%kotlin.artifacts.repository%" >> gradle.properties
                echo "kotlin.experimental.tryK2=true" >> gradle.properties
                echo "kotlin.native.home=%user.project.kotlin.native.home%" >> gradle.properties
            """.trimIndent()
        }
        gradle {
            name = "build shared part"
            tasks = "clean build"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = "--stacktrace --info --configuration-cache-problems=warn"
        }
        script {
            name = "build IOS part"
            workingDir = "user-project"
            scriptContent = "%tools.xcode.home%/usr/bin/xcodebuild -workspace ${'$'}(pwd)/ios/KaMPKitiOS.xcworkspace -scheme KaMPKitiOS -sdk iphonesimulator "
        }
    }

    failureConditions {
        executionTimeoutMin = 30
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
        }
    }

    dependencies {
        dependency(_Self.buildTypes.Artifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = """
                    +:maven.zip!**=>artifacts/kotlin
                    +:experimental-compose.zip!**=>artifacts/kotlin
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz!** => artifacts/native"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        contains("system.cloud.profile_id", "titan-up-k8s")
        noLessThanVer("tools.xcode.version", "15.1")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "7000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "9000")
    }
})

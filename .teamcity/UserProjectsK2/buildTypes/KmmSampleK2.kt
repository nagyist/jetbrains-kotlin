package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.retryBuild

object KmmSampleK2 : BuildType({
    name = "🍏ᵐ [Project] kmm-production-sample with K2"

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
        root(UserProjectsK2.vcsRoots.KmmSampleK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "configure gradle"
            workingDir = "user-project"
            scriptContent = """
                echo "kotlin_repo_url=%kotlin.artifacts.repository%" >> gradle.properties
                echo "kotlin_version=${BuildNumber.depParamRefs["deployVersion"]}" >> gradle.properties
                echo "kotlin_language_version=2.0" >> gradle.properties
                echo "kotlin_api_version=2.0" >> gradle.properties
                echo "kotlin.native.home=%user.project.kotlin.native.home%" >> gradle.properties
            """.trimIndent()
        }
        script {
            name = "build"
            workingDir = "user-project"
            scriptContent = "%tools.xcode.home%/usr/bin/xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -arch arm64"
        }
        script {
            name = "test"
            workingDir = "user-project"
            scriptContent = """
                id=${'$'}(xcrun simctl create ${'$'}(uuidgen) com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max ${'$'}(xcrun simctl runtime list -j | jq -r 'map(select(.platformIdentifier == "com.apple.platform.iphonesimulator"))[0].runtimeIdentifier'))
                %tools.xcode.home%/usr/bin/xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination id=${'$'}id
            """.trimIndent()
        }
    }

    triggers {
        retryBuild {
        }
    }

    failureConditions {
        executionTimeoutMin = 15
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

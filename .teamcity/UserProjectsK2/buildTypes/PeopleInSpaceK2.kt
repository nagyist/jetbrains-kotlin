package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object PeopleInSpaceK2 : BuildType({
    name = "🍏ᵐ [Project] People in space with K2"

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
        root(UserProjectsK2.vcsRoots.PeopleInSpaceK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(UserProjectsK2.vcsRoots.KspK2VCS, "+:. => ksp")
    }

    steps {
        script {
            name = "install chrome"
            scriptContent = "brew install --cask google-chrome"
            param("teamcity.step.phase", "bootstrap")
        }
        gradle {
            name = "build KSP"
            tasks = "publishToMavenLocal"
            buildFile = "build.gradle.kts"
            workingDir = "ksp"
            gradleParams = """
                --stacktrace --info
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                -PkotlinBaseVersion=${BuildNumber.depParamRefs["deployVersion"]}
                -Pkotlin_language_version=1.9
                -Pkotlin.native.home=%user.project.kotlin.native.home%
            """.trimIndent()
        }
        script {
            name = "configure gradle"
            workingDir = "user-project"
            scriptContent = """
                echo "org.gradle.java.home=%env.JDK_17_0%" >> gradle.properties
                echo "kotlin_version=${BuildNumber.depParamRefs["deployVersion"]}" >> gradle.properties
                echo "kotlin_repo_url=%kotlin.artifacts.repository%" >> gradle.properties
                echo "kotlin_language_version=2.0" >> gradle.properties
                echo "kotlin_api_version=2.0" >> gradle.properties
                echo "ksp_api_version=1.9" >> gradle.properties
                echo "ksp_language_version=1.9" >> gradle.properties
                echo "ksp_version=2.0.255-SNAPSHOT" >> gradle.properties
                echo "kotlin.native.home=%user.project.kotlin.native.home%" >> gradle.properties
            """.trimIndent()
        }
        gradle {
            name = "check builds and unit tests"
            tasks = ":app:assembleDebug :app:testDebugUnitTest :backend:assemble :graphql-server:assemble :wearApp:assembleDebug :web:assemble"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = "--stacktrace"
        }
        gradle {
            name = "clean after previous task"
            tasks = "clean"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = "--stacktrace"
        }
        gradle {
            name = "build and test common gradle project"
            tasks = ":common:assemble :common:allTest"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = "--stacktrace"
        }
        script {
            name = "build IOS app"
            workingDir = "user-project"
            scriptContent = "%tools.xcode.home%/usr/bin/xcodebuild build -project PeopleInSpaceSwiftUI/PeopleInSpaceSwiftUI.xcodeproj -scheme PeopleInSpaceSwiftUI -sdk iphonesimulator"
        }
        script {
            name = "run UI tests for iOS app"
            workingDir = "user-project"
            scriptContent = """
                id=${'$'}(xcrun simctl create ${'$'}(uuidgen) com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max ${'$'}(xcrun simctl runtime list -j | jq -r 'map(select(.platformIdentifier == "com.apple.platform.iphonesimulator"))[0].runtimeIdentifier'))
                %tools.xcode.home%/usr/bin/xcodebuild test -project PeopleInSpaceSwiftUI/PeopleInSpaceSwiftUI.xcodeproj -scheme PeopleInSpaceSwiftUI -destination id=${'$'}id
            """.trimIndent()
        }
        script {
            name = "build macOS app"
            workingDir = "user-project"
            scriptContent = "%tools.xcode.home%/usr/bin/xcodebuild build -project PeopleInSpaceSwiftUI/PeopleInSpaceSwiftUI.xcodeproj -scheme PeopleInSpaceMac"
        }
        script {
            name = "run UI tests for watchOS app"
            workingDir = "user-project"
            scriptContent = """
                id=${'$'}(xcrun simctl create ${'$'}(uuidgen) com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-5-40mm ${'$'}(xcrun simctl runtime list -j | jq -r 'map(select(.platformIdentifier == "com.apple.platform.watchsimulator"))[0].runtimeIdentifier'))
                %tools.xcode.home%/usr/bin/xcodebuild test -project PeopleInSpaceSwiftUI/PeopleInSpaceSwiftUI.xcodeproj -scheme "PeopleInSpaceWatch Watch App" -destination id=${'$'}id
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 45
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
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KmShopK2 : BuildType({
    name = "🍏ᵐ [Project] km-shop with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /Users/admin/buildAgent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("env.ANDROID_HOME", "%env.HOME%/Library/Android/sdk")
        param("env.PATH", "%teamcity.build.checkoutDir%/user-project/gems_bin:%env.HOME%/.rbenv/shims:%env.PATH%")
        param("env.LANG", "en_US.UTF-8")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.KmShopK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "install ruby"
            scriptContent = """
                brew install rbenv
                rbenv install 3.2.2
                rbenv global 3.2.2
            """.trimIndent()
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install cocoapods"
            workingDir = "user-project"
            scriptContent = """
                rbenv exec bundle config set bin gems_bin
                rbenv exec bundle install
            """.trimIndent()
        }
        script {
            name = "configure gradle"
            workingDir = "user-project"
            scriptContent = """
                echo "\n" >> gradle.properties
                echo "kotlin_version=${BuildNumber.depParamRefs["deployVersion"]}" >> gradle.properties
                echo "kotlin_repo_url=%kotlin.artifacts.repository%" >> gradle.properties
                echo "kotlin.native.home=%user.project.kotlin.native.home%" >> gradle.properties
            """.trimIndent()
        }
        gradle {
            name = "Shared build"
            tasks = ":shared:build"
            workingDir = "user-project"
            gradleParams = "--stacktrace --info"
        }
        gradle {
            name = "Shop server api build"
            tasks = ":shop:server:api:build"
            workingDir = "user-project"
            gradleParams = "--stacktrace --info"
        }
        gradle {
            name = "Generate dummy framework for CocoaPods integration"
            tasks = ":shared:generateDummyFramework"
            workingDir = "user-project"
            gradleParams = "--stacktrace --info"
        }
        script {
            name = "install pods"
            workingDir = "user-project/shop/mobile/ios"
            scriptContent = "pod install --repo-update"
        }
        script {
            name = "test"
            workingDir = "user-project/shop/mobile/ios"
            scriptContent = """
                id=${'$'}(xcrun simctl create ${'$'}(uuidgen) com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max ${'$'}(xcrun simctl runtime list -j | jq -r 'map(select(.platformIdentifier == "com.apple.platform.iphonesimulator"))[0].runtimeIdentifier'))
                %tools.xcode.home%/usr/bin/xcodebuild test -workspace Shop.xcworkspace -scheme Shop -destination id=${'$'}id
            """.trimIndent()
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
                artifactRules = "+:maven.zip!**=>artifacts/kotlin"
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

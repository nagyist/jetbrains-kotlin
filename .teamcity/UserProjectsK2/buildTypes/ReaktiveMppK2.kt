package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object ReaktiveMppK2 : BuildType({
    name = "🍏ᵐ [Project] Reaktive with K2"

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
        root(UserProjectsK2.vcsRoots.ReaktiveMppK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "install chrome"
            scriptContent = "brew install --cask google-chrome"
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install firefox"
            scriptContent = "brew install --cask firefox"
            param("teamcity.step.phase", "bootstrap")
        }
        gradle {
            name = "build and run tests"
            tasks = "build"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --stacktrace --info
                                        --configuration-cache-problems=warn
                                        -Pkotlin_repo_url=%kotlin.artifacts.repository%
                                        -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin.experimental.tryK2=true
                                        -Pkotlin.native.home=%user.project.kotlin.native.home%
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
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

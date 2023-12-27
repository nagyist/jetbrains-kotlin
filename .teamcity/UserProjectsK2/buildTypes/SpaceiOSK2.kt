package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object SpaceiOSK2 : BuildType({
    name = "🍏ᵐ [Project] Space iOS with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /Users/admin/buildAgent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        password("env.JB_SPACE_CLIENT_SECRET", "credentialsJSON:6d2dd689-93a7-4d0b-8175-4f470c0639c0")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}")
        param("env.JB_SPACE_CLIENT_ID", "%space.packages.user%")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.SpaceK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "linkDebugFrameworkIos"
            tasks = ":app:app-ios-native:linkDebugFrameworkIos"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --stacktrace --info
                                        --configuration-cache-problems=warn
                                        --init-script ../community-project-plugin/community-project.init.gradle.kts
                                        --init-script ../community-project-plugin/cache-redirector.init.gradle
                                        -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository%
                                        -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pcommunity.project.kotlin.languageVersion=2.0 
                                        -Pcommunity.project.kotlin.apiVersion=2.0
                                        -Pkotlin.native.home=%user.project.kotlin.native.home%
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 60
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

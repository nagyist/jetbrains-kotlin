package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinPoetK2 : BuildType({
    name = "🐧 [Project] kotlinpoet with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /mnt/agent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("docker.image", "amazoncorretto:11.0.18")
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.KotlinPoetK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(UserProjectsK2.vcsRoots.KspK2VCS, "+:. => ksp")
    }

    steps {
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
                -Dmaven.repo.local=/opt/m2
            """.trimIndent()
            dockerImage = "%docker.image%"
            dockerRunParameters = "--volume %teamcity.build.checkoutDir%/m2:/opt/m2"
        }
        gradle {
            name = "build"
            tasks = "clean build"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = """
                --stacktrace --info
                -x check -x test 
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                -Pkotlin_language_version=2.0
                -Pkotlin.native.home=%user.project.kotlin.native.home%
                -Dmaven.repo.local=/opt/m2
            """.trimIndent()
            dockerImage = "%docker.image%"
            dockerRunParameters = "--volume %teamcity.build.checkoutDir%/m2:/opt/m2"
        }
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
        }
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "kotlin-release-k2"
                messageFormat = verboseMessageFormat {
                    addChanges = true
                    addStatusText = true
                    maximumNumberOfChanges = 5
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            firstFailureAfterSuccess = true
        }
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_774"
            }
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
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz!** => artifacts/native"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "titan-up-aws")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("system.cloud.profile_id", "titan")
    }
})

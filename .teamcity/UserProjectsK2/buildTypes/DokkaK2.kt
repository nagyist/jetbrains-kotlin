package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object DokkaK2 : BuildType({
    name = "🐧 [Project] Dokka with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /mnt/agent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("docker.image", "amazoncorretto:17")
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.DokkaK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "Build"
            tasks = "build"
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
                                       -Pcommunity.project.disable.verification.tasks=true
                                       -Pcommunity.project.ignore.dependencies.names=high-level-api-for-ide,high-level-api-impl-base-for-ide,high-level-api-fir-for-ide,high-level-api-fe10-for-ide,low-level-api-fir-for-ide,analysis-project-structure-for-ide,analysis-api-standalone-for-ide,analysis-api-providers-for-ide,symbol-light-classes-for-ide
                                       -Pcommunity.project.gradle.repositories.mode=settings
                                       -Pcommunity_plugin_path=../../community-project-plugin
            """.trimIndent()
            dockerImage = "%docker.image%"
            dockerRunParameters = "--user=%env.UID%:%env.UID% --volume /etc/passwd:/etc/passwd --volume %env.HOME%:%env.HOME%"
        }
    }

    failureConditions {
        executionTimeoutMin = 20
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
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

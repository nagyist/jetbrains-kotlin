package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object NowinandroidK2 : BuildType({
    name = "🐧 [Project] nowinandroid with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /mnt/agent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("docker.image", "registry.jetbrains.team/p/kct/containers/android_jdk17:x86_latest")
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("system.test.agp_version", "8.2.0-alpha15")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}")
        param("system.test.ksp_version", "2.0.255-SNAPSHOT")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.NowinandroidVCS, "+:. => user-project")
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
            tasks = "build"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = """
                -x :app:lintDemoDebug
                                        --stacktrace --info
                                        --configuration-cache-problems=warn
                                        -Pkotlin_repo_url=%kotlin.artifacts.repository%
                                        -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pksp=%system.test.ksp_version%
                                        -Pagp=%system.test.agp_version%
                                        -Pkotlin.experimental.tryK2=true
                                        -Pkotlin.native.home=%user.project.kotlin.native.home%
                                        -Dmaven.repo.local=/opt/m2
            """.trimIndent()
            dockerImage = "%docker.image%"
            dockerRunParameters = "--volume %teamcity.build.checkoutDir%/m2:/opt/m2"
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
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_1334"
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
                artifactRules = """
                    +:maven.zip!**=>artifacts/kotlin
                    +:experimental-compose.zip!**=>artifacts/kotlin
                """.trimIndent()
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

package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object kotlinxAtomicfu : BuildType({
    name = "🐧 [Project] kotlinx.atomicfu K1"
    description = """
        https://jetbrains.team/p/kti/documents/All/a/Merge-Request-to-Kotlin-Repository-Process-community-projects-co#kotlinx-libraries-compilation-fix-process
        kotlinx.atomicfu settings:
        VCS URL: git@github.com:Kotlin/kotlinx-atomicfu.git
        VCS branch: kotlin-community/dev
        Deploy Kotlin Maven Artifacts (MANUAL): https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_BuildPlayground_Titan_DeployMavenArtifacts
        gradle command should be executed: 
        ./gradlew clean build --stacktrace --info
                    -Pkotlin.native.home=/path/to/unpacked/bundle                     
                    -x :atomicfu:check -x :atomicfu-gradle-plugin:test 
                    -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev 
                    -Pkotlin_version=[kotlinVersion]
                    -Pkotlin_language_version=1.9
    """.trimIndent()

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /mnt/agent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}")
    }

    vcs {
        root(Tests_Linux.vcsRoots.AtomicfuForAggregateVCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --stacktrace --info
                                        -x :atomicfu:check -x :atomicfu-gradle-plugin:test 
                                        -Pkotlin.native.home=%user.project.kotlin.native.home% 
                                        -Pkotlin_repo_url=%kotlin.artifacts.repository%
                                        -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]} 
                                        -Pkotlin_language_version=1.9
            """.trimIndent()
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Process description in case of build failures"
            executionMode = BuildStep.ExecutionMode.RUN_ONLY_ON_FAILURE
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                echo "##teamcity[buildStatus text='More info: https://jetbrains.team/p/kti/documents/All/a/Merge-Request-to-Kotlin-Repository-Process-community-projects-co#kotlinx-libraries-compilation-fix-process {build.status.text}']"
            """.trimIndent()
        }
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
        }
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
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
        contains("system.cloud.profile_id", "aws")
        startsWith("system.cloud.profile_id", "titan")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

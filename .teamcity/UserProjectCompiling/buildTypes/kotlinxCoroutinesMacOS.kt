package UserProjectCompiling.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object kotlinxCoroutinesMacOS : BuildType({
    name = "🍏ᵐ [Project] kotlinx.coroutines (MacOS)"
    description = """
        https://jetbrains.team/p/kti/documents/All/a/Merge-Request-to-Kotlin-Repository-Process-community-projects-co#kotlinx-libraries-compilation-fix-process
        kotlinx.coroutines settings:
        VCS URL: git@github.com:Kotlin/kotlinx-coroutines.git
        VCS branch: kotlin-community/dev
        Deploy Kotlin Maven Artifacts (MANUAL): https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_BuildPlayground_Titan_DeployMavenArtifacts
        gradle command should be executed: 
        ./gradlew build --stacktrace --info 
                -x check 
                -x kotlinStoreYarnLock 
                -Pkotlin.native.home=/path/to/unpacked/bundle
                -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev 
                -Pkotlin_snapshot_version=[kotlinVersion] 
                -Pkotlin_version=[kotlinVersion]
                -Pkotlin_language_version=1.9
                -Pkotlin_api_version=1.9
    """.trimIndent()

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /Users/admin/buildAgent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("env.CACHE_REDIRECTOR", "true")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}")
    }

    vcs {
        root(_Self.vcsRoots.coroutinesForAggregateVCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "build project"
            tasks = "build"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --stacktrace --info
                                        --init-script ../community-project-plugin/community-project.init.gradle.kts
                                        --init-script ../community-project-plugin/cache-redirector.init.gradle
                                        -Pcommunity.project.disable.verification.tasks=true
                                        -x jvmApiCheck
                                        -x kotlinStoreYarnLock 
                                        -Pkotlin.native.home=%user.project.kotlin.native.home% 
                                        -Pkotlin_repo_url=%kotlin.artifacts.repository%
                                        -Pkotlin_snapshot_version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin_language_version=1.9
                                        -Pkotlin_api_version=1.9
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
    }
})

package KotlinxTrain.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinxTrainKtor : BuildType({
    name = "🐧 ktorio.ktor"

    artifactRules = """
        %train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx
        **/*.hprof=>internal/hprof.zip
    """.trimIndent()
    buildNumberPattern = "%DeployVersion% (%build.counter%)"

    params {
        param("train.maven.local.repository.path", "%teamcity.build.checkoutDir%/%train.maven.local.repository.dir%")
        param("branch.ktor", "kotlin-community/dev")
        param("DeployVersion", "2.2.3-train-%kotlin_snapshot_version%")
        param("kotlin_snapshot_version", "${KotlinxTrainAtomicfu.depParamRefs["kotlin_snapshot_version"]}")
        param("train.maven.local.repository.dir", ".localrepo")
    }

    vcs {
        root(UserProjectCompiling.vcsRoots.ktorioktor, "+:. => project.ktor")
    }

    steps {
        script {
            name = "Accept Android SDK license (required for ktor)"
            scriptContent = "yes | JAVA_HOME=%env.JDK_1_8% %env.ANDROID_SDK_HOME%/cmdline-tools/latest/bin/sdkmanager --licenses"
        }
        gradle {
            name = "Build ktor project"
            tasks = "build"
            buildFile = "build.gradle.kts"
            workingDir = "project.ktor"
            gradleParams = """
                -x apiCheck -x kotlinStoreYarnLock
                -Pkotlin_version=%kotlin_snapshot_version%
                -Pkotlin_snapshot_version=%kotlin_snapshot_version%
                -Patomicfu_version=%DeployVersion%
                -Pcoroutines_version=%DeployVersion%
                -Pserialization_version=%DeployVersion% 
                -Pkotlin_language_version=1.9 
                -Pbuild_snapshot_train=true 
                -Dmaven.repo.local=%train.maven.local.repository.path% 
                -PDeployVersion=%DeployVersion% 
                --no-parallel
                --stacktrace
                --continue
            """.trimIndent()
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 100
        supportTestRetry = true
    }

    dependencies {
        snapshot(KotlinxTrainAtomicfu) {
        }
        snapshot(KotlinxTrainCoroutinesBuildAndTests) {
        }
        dependency(KotlinxTrainCoroutinesMavenArtifactsOnly) {
            snapshot {
            }

            artifacts {
                artifactRules = "%train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx"
            }
        }
        dependency(KotlinxTrainSerialization) {
            snapshot {
            }

            artifacts {
                artifactRules = "%train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx"
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

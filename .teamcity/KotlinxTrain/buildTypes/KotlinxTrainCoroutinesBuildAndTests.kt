package KotlinxTrain.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinxTrainCoroutinesBuildAndTests : BuildType({
    name = "🐧 kotlinx.coroutines (build and integration tests)"

    artifactRules = """
        %train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx
        **/*.hprof=>internal/hprof.zip
    """.trimIndent()
    buildNumberPattern = "%DeployVersion% (%build.counter%)"

    params {
        param("train.maven.local.repository.path", "%teamcity.build.checkoutDir%/%train.maven.local.repository.dir%")
        param("DeployVersion", "2.2.3-train-%kotlin_snapshot_version%")
        param("branch.coroutines", "kotlin-community/dev")
        param("kotlin_snapshot_version", "")
        param("train.maven.local.repository.dir", ".localrepo")
    }

    vcs {
        root(UserProjectCompiling.vcsRoots.kotlinxcoroutines_1, "+:. => project.coroutines")
    }

    steps {
        gradle {
            name = "Build coroutines project"
            tasks = "build"
            buildFile = "build.gradle"
            workingDir = "project.coroutines"
            gradleParams = """
                --stacktrace 
                -x check -x test
                -x kotlinStoreYarnLock 
                -Patomicfu_version=%DeployVersion% 
                -Pkotlin_version=%kotlin_snapshot_version% 
                -Pkotlin_snapshot_version=%kotlin_snapshot_version% 
                -Pkotlin_language_version=1.9 
                -Pskip_snapshot_checks=true 
                -Dmaven.repo.local=%train.maven.local.repository.path% 
                -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev 
                --info 
                --continue 
                -Pbuild_snapshot_train=true 
                -PDeployVersion=%DeployVersion%
            """.trimIndent()
            jdkHome = "%env.JDK_11%"
        }
        gradle {
            name = "Run integration tests"
            tasks = "check"
            buildFile = "build.gradle"
            workingDir = "project.coroutines/integration-testing"
            gradleParams = """
                -x kotlinStoreYarnLock 
                -x debugAgentTest 
                -Patomicfu_version=%DeployVersion% 
                -Pkotlin_version=%kotlin_snapshot_version% 
                -Pkotlin_snapshot_version=%kotlin_snapshot_version% 
                -Pkotlin_language_version=1.9 
                -Pskip_snapshot_checks=true 
                -Dmaven.repo.local=%train.maven.local.repository.path% 
                -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev 
                -Pbuild_snapshot_train=true 
                -PDeployVersion=%DeployVersion%
                -Pcoroutines_version=%DeployVersion% 
                -Dorg.gradle.caching=false 
                -Dorg.gradle.parallel=false 
                -Dorg.gradle.jvmargs=-Xmx4g 
                --stacktrace --info
            """.trimIndent()
            jdkHome = "%env.JDK_11%"
        }
    }

    failureConditions {
        executionTimeoutMin = 45
        supportTestRetry = true
    }

    dependencies {
        dependency(KotlinxTrainAtomicfu) {
            snapshot {
            }

            artifacts {
                artifactRules = "%train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx"
            }
        }
        dependency(KotlinxTrainCoroutinesMavenArtifactsOnly) {
            snapshot {
            }

            artifacts {
                artifactRules = "%train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        moreThan("teamcity.agent.hardware.memorySizeMb", "6000")
        noLessThanVer("teamcity.agent.jvm.specification", "1.8")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "4000")
        contains("system.cloud.profile_id", "aws")
        startsWith("system.cloud.profile_id", "titan")
    }
})

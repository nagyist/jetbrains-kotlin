package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object CompilerTests21JDK : BuildType({
    name = "🐧 Compiler Tests (21 JDK)"
    description = "A temporal configuration for testing compiler with JDK 21, while JDK 21 is not released and thus not available for accessing with Gradle Toolchain feature"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("docker.image", "registry.jetbrains.team/p/kct/containers/kotlin-tests-jdk21-amd64:latest")
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("testTasks", ":compiler:tests-common-new:jdk21Tests :compiler:fir:analysis-tests:jdk21Tests")
        param("gradleParameters", "%globalGradleParameters% -PdeployVersion=${BuildNumber.depParamRefs["deployVersion"]} -Pbuild.number=%build.number%")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Build tests"
            tasks = "%testTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Pkotlin.build.disable.verification.tasks=true -Dscan.uploadInBackground=false -g %env.GRADLE_USER_HOME%"
            enableStacktrace = false
            dockerImage = "%docker.image%"
        }
        gradle {
            name = "Run tests"
            tasks = "%testTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Dscan.uploadInBackground=false -g %env.GRADLE_USER_HOME%"
            enableStacktrace = false
            dockerImage = "%docker.image%"
        }
    }

    failureConditions {
        executionTimeoutMin = 30
        supportTestRetry = true
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_1334"
            }
        }
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(_Self.buildTypes.CompileAllClasses) {
            onDependencyFailure = FailureAction.FAIL_TO_START
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

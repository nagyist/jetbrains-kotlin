package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.maven

object ComposeArtifacts : BuildType({
    name = "🐧 Compose Artifacts"
    description = "Read more about experimental compose and how to fix this configuration at https://jetbrains.team/p/kti/documents/All/a/Compose-in-Aggregate"

    artifactRules = """
        kotlin/build/local-compose-publish=>experimental-compose-%DeployVersion%.zip
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("mavenParameters", "")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.CompatibleComposeVCS, "+:. => kotlin/plugins/compose-compiler-plugin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        maven {
            name = "Set Version"
            goals = "versions:set"
            pomLocation = "%teamcity.build.checkoutDir%/kotlin/libraries/pom.xml"
            runnerArgs = "-DnewVersion=%DeployVersion% -DgenerateBackupPoms=false -DprocessAllModules=true"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            mavenVersion = bundled_3_6()
            localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Publish Compose"
            tasks = "publish"
            buildFile = "build.gradle.kts"
            workingDir = "kotlin"
            gradleParams = """%gradleParameters% --no-parallel -p plugins/compose-compiler-plugin -Pkotlin.build.disable.werror=true -Pkotlin.build.compose.publish.enabled=true "-PdeployVersion=%DeployVersion%" "-Pbuild.number=%build.number%" "-Pdeploy-repo=local" "-Pdeploy-url=file://%teamcity.build.checkoutDir%/kotlin/build/local-compose-publish" --no-scan"""
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 20
    }

    features {
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(CompilerDist) {
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

package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object CompilerDistAndMavenArtifactsForIde : BuildType({
    name = "🐧 Compiler Dist and Maven Artifacts for IDE (Language Version 1.9)"

    artifactRules = "kotlin/build/repo => maven.zip"
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "%build.number%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        param("system.deployVersion", "%DeployVersion%")
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("mavenParameters", "")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Check that 'teamcity.build.branch' isn't ignored"
            scriptContent = """
                EXPECTED_BRANCH="refs/heads/%teamcity.build.branch%"
                ACTUAL_BRANCH="${DslContext.settingsRoot.paramRefs.buildVcsBranch}"
                if [ "${'$'}EXPECTED_BRANCH" != "${'$'}ACTUAL_BRANCH" ]; then
                    >&2 echo "teamcity.build.branch = ${'$'}EXPECTED_BRANCH is ignored and ${'$'}ACTUAL_BRANCH is used instead"
                    exit 1
                fi
            """.trimIndent()
        }
        gradle {
            name = "Publish compiler dist artifact"
            tasks = ":prepare:ide-plugin-dependencies:kotlin-dist-for-ide:publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Dorg.gradle.internal.publish.checksums.insecure=true -Ppublish.ide.plugin.dependencies=true -Pkotlin.native.enabled=true -PkotlinLanguageVersion=1.9"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Publish artifacts for IDE"
            tasks = "publishIdeArtifacts"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Dorg.gradle.internal.publish.checksums.insecure=true -Ppublish.ide.plugin.dependencies=true -Pkotlin.native.enabled=true -PkotlinLanguageVersion=1.9"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Publish artifacts for Kotlin Gradle Plugin"
            tasks = "publishGradlePluginArtifacts"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Dorg.gradle.internal.publish.checksums.insecure=true -Ppublish.ide.plugin.dependencies=true -Pkotlin.native.enabled=true -PkotlinLanguageVersion=1.9"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 90
        errorMessage = true
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
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "aws")
        startsWith("system.cloud.profile_id", "titan")
    }
})

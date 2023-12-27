package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object DeployIdePluginDependenciesMavenArtifacts : BuildType({
    name = "🐧 Deploy IDE plugin dependencies (Language Version 1.9)"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        text("DeployVersion", "${BuildNumber.depParamRefs.buildNumber}", display = ParameterDisplay.PROMPT)
        param("deploy-repo", "kotlin-space-packages")
        param("deploy-url", "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        password("system.kotlin.kotlin-space-packages.user", "credentialsJSON:317fd9a0-00c6-45a3-8046-66e39aef39c1", display = ParameterDisplay.HIDDEN)
        param("mavenParameters", "")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        password("system.kotlin.kotlin-space-packages.password", "credentialsJSON:909b7ec4-7b0b-4868-ac5c-637a8fb9dbfe", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Publish compiler dist artifact"
            tasks = ":prepare:ide-plugin-dependencies:kotlin-dist-for-ide:publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel -PdeployVersion=%DeployVersion% -Pdeploy-repo=%deploy-repo% -Pdeploy-url=%deploy-url% -Dorg.gradle.internal.publish.checksums.insecure=true -Ppublish.ide.plugin.dependencies=true -Pkotlin.native.enabled=true -PkotlinLanguageVersion=1.9 --no-scan"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Gradle install and publish"
            tasks = "publishIdeArtifacts"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel -PdeployVersion=%DeployVersion% -Pdeploy-repo=%deploy-repo% -Pdeploy-url=%deploy-url% -Dorg.gradle.internal.publish.checksums.insecure=true -Ppublish.ide.plugin.dependencies=true -Pkotlin.native.enabled=true -PkotlinLanguageVersion=1.9 --no-scan"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 90
        errorMessage = true
    }

    features {
        swabra {
            lockingProcesses = Swabra.LockingProcessPolicy.KILL
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
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "aws")
        startsWith("system.cloud.profile_id", "titan")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }

    cleanup {
        keepRule {
            id = "Keep all history and statistics for `Deploy IDE plugin dependencies (Language Version 1.9)`"
            keepAtLeast = allBuilds()
            dataToKeep = historyAndStatistics()
        }
    }
})

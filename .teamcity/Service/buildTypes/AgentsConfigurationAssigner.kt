package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object AgentsConfigurationAssigner : BuildType({
    name = "🐧 Agents Configuration Assigner"

    params {
        param("gradleParameters", "%globalGradleParameters%")
    }

    vcs {
        root(Service.vcsRoots.AgentsConfigurationAssigner_1, "+:. => agents-configuration-assigner")
    }

    steps {
        gradle {
            name = "run assigner task"
            tasks = ":run"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/agents-configuration-assigner"
            gradleParams = "%gradleParameters% --parallel -Ptoken=%teamcity.serviceUser.token%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    triggers {
        vcs {
            enabled = false
            triggerRules = "+:/src/main/resources/inventory.yaml"
            branchFilter = "+:<default>"
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

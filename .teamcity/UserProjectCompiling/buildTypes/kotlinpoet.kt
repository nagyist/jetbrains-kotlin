package UserProjectCompiling.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object kotlinpoet : BuildType({
    name = "🐧 [Project] kotlinpoet"
    description = "original repo: https://github.com/square/kotlinpoet.git#refs/heads/master"

    artifactRules = "**/*.hprof=>internal/hprof.zip"
    buildNumberPattern = "%system.kotlin_snapshot_version%-%build.counter%"

    params {
        text("kotlin_snapshot_version", "", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
        param("branch.kotlinpoet", "kotlin/dev")
        param("system.kotlin_snapshot_version", "%kotlin_snapshot_version%")
    }

    vcs {
        root(UserProjectCompiling.vcsRoots.kotlinpoet_1, "+:.=>user-project")
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = "--stacktrace -x test -PkotlinVersion=%system.kotlin_snapshot_version%"
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        supportTestRetry = true
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

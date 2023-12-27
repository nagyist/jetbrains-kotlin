package UserProjectCompiling.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object benchmark : BuildType({
    name = "🐧 [Project] kotlinx-benchmark"
    description = "Build configuration: kotlinx-benchmark"

    artifactRules = "**/*.hprof=>internal/hprof.zip"
    buildNumberPattern = "%system.kotlin_snapshot_version%-%build.counter%"

    params {
        text("kotlin_snapshot_version", "", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
        param("branch.benchmark", "kotlin-community/dev")
        param("system.kotlin_snapshot_version", "%kotlin_snapshot_version%")
    }

    vcs {
        root(UserProjectCompiling.vcsRoots.kotlinxbenchmark, "+:.=>user-project")
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = "--stacktrace -PoverrideTeamCityBuildNumber=false -Pkotlin_language_version=1.9  -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev --info --continue  -Pkotlin_version=%system.kotlin_snapshot_version%"
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

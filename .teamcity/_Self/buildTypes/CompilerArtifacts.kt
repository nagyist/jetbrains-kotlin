package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object CompilerArtifacts : BuildType({
    name = "🐧 Compiler Artifacts (no caches)"

    artifactRules = "kotlin_CompilerArtifacts/dist/kotlin-compiler-*"
    buildNumberPattern = "%build.number.pattern%"

    params {
        param("build.number.pattern", "BN: ${BuildNumber.depParamRefs.buildNumber} DV: %DeployVersion%")
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        param("system.deployVersion", "%DeployVersion%")
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("kotlin.compiler.zip.name", "kotlin-compiler-%DeployVersion%.zip")
        param("mavenParameters", "")
        param("system.build.number", "${BuildNumber.depParamRefs.buildNumber}")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin_CompilerArtifacts")

        cleanCheckout = true
    }

    steps {
        script {
            name = "Prepare gnupg"
            workingDir = "kotlin_CompilerArtifacts"
            scriptContent = """
                cd .
                export HOME=${'$'}(pwd)
                export GPG_TTY=${'$'}(tty)
                
                rm -rf .gnupg
                
                cat >keyfile <<EOT
                %sign.key.private.new%
                EOT
                gpg --allow-secret-key-import --batch --import keyfile
                rm -v keyfile
                
                cat >keyfile <<EOT
                %sign.key.main.public%
                EOT
                gpg --batch --import keyfile
                rm -v keyfile
            """.trimIndent()
        }
        gradle {
            name = "Zip compiler with signature and checksum"
            tasks = "zipCompilerWithSignature"
            buildFile = "build.gradle.kts"
            workingDir = "kotlin_CompilerArtifacts"
            gradleParams = "%gradleParameters% --parallel -PsigningRequired=true -Psigning.gnupg.executable=gpg -Psigning.gnupg.keyName=%sign.key.id.new% -Psigning.gnupg.homeDir=.gnupg -Psigning.gnupg.passphrase=%sign.key.passphrase.new% --no-build-cache"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Cleanup"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            workingDir = "kotlin_CompilerArtifacts"
            scriptContent = """
                cd .
                rm -rf .gnupg
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 120
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
        contains("system.cloud.profile_id", "aws")
        startsWith("system.cloud.profile_id", "titan")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon

object Artifacts : BuildType({
    name = "🐧 Artifacts"

    artifactRules = """
        +:**/*
        +:experimental-compose=>experimental-compose.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
        cleanCheckout = true
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
        dependency(CompilerDistAndMavenArtifacts) {
            snapshot {
            }

            artifacts {
                artifactRules = """
                    internal/**=>internal
                    maven.zip!**=>maven
                    maven.zip
                    kotlin-compiler-*
                """.trimIndent()
            }
        }
        dependency(ComposeArtifacts) {
            snapshot {
            }

            artifacts {
                artifactRules = "experimental-compose*.zip!**=>experimental-compose"
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_BUNDLE) {
            snapshot {
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-linux-x86_64-*"
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

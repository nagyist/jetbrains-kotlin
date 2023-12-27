package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import _Self.buildTypes.ComposeArtifacts
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object DeployCompose : BuildType({
    name = "🐧 Deploy Compose"
    description = "Deploy experimental compose artifacts to Space https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/experimental/compose/.Read more about experimental compose at https://jetbrains.team/p/kti/documents/All/a/Compose-in-Aggregate"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("publishing-util-version", "0.1.95")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.CompatibleComposeVCS, "+:. => kotlin/plugins/compose-compiler-plugin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Upload Compose"
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                ./space-cli/bin/space-cli \
                    upload \
                        --url https://kotlin.jetbrains.space \
                        --username %space.kotlin.packages.user% \
                        --password %space.kotlin.packages.secret% \
                        --project KOTLIN \
                        --repo dev \
                        --dir maven-compose \
                        --versions "${ComposeArtifacts.depParamRefs["DeployVersion"]}"
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 10
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
        dependency(_Self.buildTypes.ComposeArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = """
                    +:experimental-compose*.zip!**=>maven-compose/${ComposeArtifacts.depParamRefs["DeployVersion"]}
                    -:experimental-compose*.zip!/org/**/maven-metadata.xml*
                """.trimIndent()
            }
        }
        artifacts(AbsoluteId("Kotlin_ServiceTasks_BintrayUtils_Build")) {
            buildRule = build("%publishing-util-version%")
            artifactRules = "space-cli-%publishing-util-version%.zip!/space-cli-%publishing-util-version%/**=>space-cli"
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

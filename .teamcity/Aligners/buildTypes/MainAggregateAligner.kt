package Aligners.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object MainAggregateAligner : BuildType({
    name = "🐧 Aligner for Full aggregate"
    description = """
        Align Kotlin and IntelliJ repos for cooperative build.
        More information: https://jetbrains.team/p/kti/documents/All/a/Aligner
    """.trimIndent()

    params {
        param("teamcity.perfmon.feature.enabled", "false")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Set align status"

            conditions {
                equals("teamcity.build.branch.is_default", "false")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                full_commit_hash=${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit"]}
                short_commit_hash=${'$'}{full_commit_hash:0:6}
                
                echo "##teamcity[buildStatus text='${AlignPushLatest.depParamRefs["push_status"]} ${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit_date"]} kt-${'$'}short_commit_hash {build.status.text}']"
            """.trimIndent()
        }
        script {
            name = "Set align status for default"

            conditions {
                equals("teamcity.build.branch.is_default", "true")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                echo "##teamcity[buildStatus text='No align for default branch {build.status.text}']"
            """.trimIndent()
        }
        script {
            name = "Trigger desired build"

            conditions {
                equals("teamcity.build.branch.is_default", "false")
            }
            scriptContent = """
                #!/bin/bash
                                    set -e
                                    set -x
                                    
                                    curl -X POST %teamcity.serverUrl%/app/rest/buildQueue \
                                    -H "Authorization: Bearer %teamcity.serviceUser.token%" \
                                    -H "Content-Type: application/xml" \
                                    -H "Origin: %teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%" \
                                    -d '
                                    <build branchName="%teamcity.build.branch%" comment="Started from %teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%">
                                        <buildType id="Kotlin_BuildPlayground_Titan_Aggregate"/>
                                        <properties>
                                            <property name="teamcity.build.triggeredBy" value="%teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%"/>
                                            <property name="teamcity.build.triggeredBy.username" value="%teamcity.build.triggeredBy.username% - Rest API teamcity.serviceUser.token"/>
                                        </properties>
                                    </build>'
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 120
            branchFilter = """
                +:<default>
                +:rr/*
                +:rrn/*
                +:push/*
            """.trimIndent()
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "jb.team -_ Applications -_ Kotlin Infrastructure Auto-Push"
        }
    }

    dependencies {
        snapshot(AlignDivergeCommitSearch) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(AlignPushLatest) {
            reuseBuilds = ReuseBuilds.NO
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

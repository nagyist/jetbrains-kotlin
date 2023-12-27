package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object MavenArtifactsAgent : BuildType({
    name = "🐧 Maven Artifacts Agent (no cache, native override)"
    description = "Run build for Maven artefacts with the scripts/build-kotlin-maven.sh"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        kotlin/build/repo-reproducible/%reproducible.maven.artifact%
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("kotlin_build_number", "%build.number%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("kotlin_deploy_version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin_native_version", "-- set in 'Setup latest kotlin native version' step --")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("reproducible.maven.artifact", "reproducible-maven-%kotlin_deploy_version%.zip")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        ant {
            name = "Setup latest kotlin native version"

            conditions {
                equals("kotlin_native_version", "-- set in 'Setup latest kotlin native version' step --")
            }
            mode = antScript {
                content = """
                    <project name="Kotlin" default="setup-latest-kotlin-native-version">
                        <target name="setup-latest-kotlin-native-version">
                            <get src="https://buildserver.labs.intellij.net/guestAuth/app/rest/builds/buildType:(Kotlin_KotlinDev_KotlinNativePublish),status:success,count:1/number" dest="latest-kotlin-native-version.txt" />
                            <loadfile property="latest-kotlin-native-version" srcfile="latest-kotlin-native-version.txt"/>
                            <echo message="##teamcity[setParameter name='kotlin_native_version' value='${'$'}{latest-kotlin-native-version}']" />
                            <delete file="latest-kotlin-native-version.txt"/>
                        </target>
                    </project>
                """.trimIndent()
            }
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
        }
        script {
            name = "Build Maven Reproducible"
            workingDir = "kotlin"
            scriptContent = """
                export PATH=${'$'}PATH:"%teamcity.tool.maven3_6%/bin"
                export JAVA_HOME=%env.JDK_11_0%
                ./scripts/build-kotlin-maven.sh %kotlin_deploy_version% '%kotlin_build_number%' %kotlin_native_version%
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

package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object RealmKotlinK2 : BuildType({
    name = "🍏ᵐ [Project] Realm Kotlin with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /Users/admin/buildAgent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}")
        param("env.ANDROID_HOME", "%env.HOME%/Library/Android/sdk")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.RealmKotlinK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "install ninja"
            workingDir = "user-project"
            scriptContent = "brew install ninja"
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install ccache"
            workingDir = "user-project"
            scriptContent = "brew install ccache"
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install swig"
            workingDir = "user-project"
            scriptContent = "brew install swig"
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "configure gradle"
            workingDir = "user-project"
            scriptContent = """
                echo "org.gradle.java.home=%env.JDK_17_0%" >> packages/gradle.properties
                echo "kotlin_repo_url=%kotlin.artifacts.repository%" | tee -a buildSrc/gradle.properties packages/gradle.properties
                echo "kotlin_version=${BuildNumber.depParamRefs["deployVersion"]}" | tee -a buildSrc/gradle.properties packages/gradle.properties
                echo "kotlin_api_version=2.0" | tee -a buildSrc/gradle.properties packages/gradle.properties
                echo "kotlin_language_version=2.0" | tee -a buildSrc/gradle.properties packages/gradle.properties
                echo "kotlin.native.home=%user.project.kotlin.native.home%" | tee -a buildSrc/gradle.properties packages/gradle.properties
            """.trimIndent()
        }
        script {
            name = "Update submodules"
            workingDir = "user-project"
            scriptContent = "git submodule update --init --recursive"
        }
        gradle {
            name = "compile"
            tasks = "publishAllPublicationsToTestRepository"
            buildFile = "build.gradle.kts"
            workingDir = "user-project/packages"
        }
    }

    failureConditions {
        executionTimeoutMin = 60
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
        }
    }

    dependencies {
        dependency(_Self.buildTypes.Artifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:maven.zip!**=>artifacts/kotlin"
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz!** => artifacts/native"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        contains("system.cloud.profile_id", "titan-up-k8s")
        noLessThanVer("tools.xcode.version", "15.1")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "7000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "9000")
    }
})

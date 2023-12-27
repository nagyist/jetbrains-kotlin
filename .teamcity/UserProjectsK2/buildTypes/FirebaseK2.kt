package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object FirebaseK2 : BuildType({
    name = "🍏ᵐ [Project] Firebase with K2"

    artifactRules = """
        **/*.hprof=>internal/hprof.zip
        **/kotlin-daemon*.log=>internal/logs.zip
        /Users/admin/buildAgent/system/**/*daemon*.log=>internal/logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("env.ANDROID_HOME", "%env.HOME%/Library/Android/sdk")
        param("env.PATH", "%teamcity.build.checkoutDir%/user-project/gems_bin:%env.HOME%/.rbenv/shims:%env.PATH%")
        param("user.project.kotlin.native.home", "%teamcity.build.checkoutDir%/artifacts/native/kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.FirebaseK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVCS, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "install node"
            scriptContent = "brew install node"
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install firebase-tools"
            workingDir = "user-project"
            scriptContent = "npm install -g firebase-tools"
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install chrome"
            scriptContent = "brew install --cask google-chrome"
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install ruby"
            scriptContent = """
                brew install rbenv
                rbenv install 3.2.2
                rbenv global 3.2.2
            """.trimIndent()
            param("teamcity.step.phase", "bootstrap")
        }
        script {
            name = "install cocoapods"
            workingDir = "user-project"
            scriptContent = """
                rbenv exec bundle config set bin gems_bin
                rbenv exec bundle install
            """.trimIndent()
        }
        script {
            name = "configure gradle"
            workingDir = "user-project"
            scriptContent = """
                echo "kotlin_repo_url=%kotlin.artifacts.repository%" >> gradle.properties
                echo "kotlin_version=${BuildNumber.depParamRefs["deployVersion"]}" >> gradle.properties
                echo "kotlin_api_version=2.0" >> gradle.properties
                echo "kotlin_language_version=2.0" >> gradle.properties
                echo "kotlin.native.home=%user.project.kotlin.native.home%" >> gradle.properties
            """.trimIndent()
        }
        gradle {
            name = "update versions"
            tasks = ":updateVersions"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
        }
        gradle {
            name = "publish to local Maven"
            tasks = "firebase-app:publishToMavenLocal firebase-auth:publishToMavenLocal firebase-common:publishToMavenLocal firebase-config:publishToMavenLocal firebase-database:publishToMavenLocal firebase-firestore:publishToMavenLocal firebase-functions:publishToMavenLocal firebase-installations:publishToMavenLocal firebase-perf:publishToMavenLocal firebase-crashlytics:publishToMavenLocal"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
        }
        gradle {
            name = "assemble"
            tasks = "assemble"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
        }
        script {
            name = "run UI emulator"
            workingDir = "user-project"
            scriptContent = "nohup firebase emulators:start --config=./test/firebase.json > /dev/null 2>&1 &"
        }
        script {
            name = "Wait for emulator to start"
            scriptContent = "sleep 60"
        }
        gradle {
            name = "run IOS tests"
            tasks = " cleanTest iosSimulatorArm64Test"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
        }
        gradle {
            name = "run JS tests"
            tasks = "cleanTest jsTest"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
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
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})

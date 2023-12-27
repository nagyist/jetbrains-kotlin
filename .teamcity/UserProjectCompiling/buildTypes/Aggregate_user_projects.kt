package UserProjectCompiling.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object Aggregate_user_projects : BuildType({
    name = "[Aggregate] All projects (artifacts from space packages)"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%reverse.dep.*.kotlin_snapshot_version%-%build.counter%"

    params {
        text("reverse.dep.*.kotlin_snapshot_version", "2.0.0", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
    }

    dependencies {
        snapshot(Exposed) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(KotlinxTrain.buildTypes.KotlinxTrainKtor) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(YouTrack) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(atomicfu) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(benchmark) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(coroutines) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(kotlinpoet) {
            reuseBuilds = ReuseBuilds.NO
        }
    }
})

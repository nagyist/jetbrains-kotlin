package UserProjectCompiling.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object Aggregate_user_projects_Kotlinx_Libraries : BuildType({
    name = "[Aggregate] Kotlinx projects"
    description = "Aggregate configuration for kotlinx libraries only"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%reverse.dep.*.kotlin_snapshot_version%-%build.counter%"

    params {
        text("reverse.dep.*.kotlin_snapshot_version", "2.0.0", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
    }

    dependencies {
        snapshot(KotlinxTrain.buildTypes.KotlinxTrainKtor) {
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
    }
})

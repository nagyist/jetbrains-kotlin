package KotlinxTrain

import KotlinxTrain.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("KotlinxTrain")
    name = "Kotlinx.train"
    description = "Building kotlinx train with kotlin compiler"

    buildType(KotlinxTrainAtomicfu)
    buildType(KotlinxTrainCoroutinesBuildAndTests)
    buildType(KotlinxTrainCoroutinesMavenArtifactsOnly)
    buildType(KotlinxTrainKtor)
    buildType(KotlinxTrainSerialization)

    params {
        text("reverse.dep.*.kotlin_snapshot_version", "2.0.0", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
    }
})

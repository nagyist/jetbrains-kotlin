package UserProjectCompiling

import UserProjectCompiling.buildTypes.*
import UserProjectCompiling.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("UserProjectCompiling")
    name = "Kotlin user projects"
    description = "Building User projects with kotlin compiler"

    vcsRoot(tachiyomi_1)
    vcsRoot(kotlinxcoroutines_1)
    vcsRoot(ktorioktor)
    vcsRoot(YouTrack_1)
    vcsRoot(kotlinpoet_1)
    vcsRoot(kotlinxbenchmark)
    vcsRoot(kotlinxatomicfu)
    vcsRoot(Exposed_1)

    buildType(UserProjectsAggregateK1)
    buildType(Aggregate_user_projects_Nightly)
    buildType(Aggregate_user_projects_Kotlinx_Libraries)
    buildType(tachiyomi)
    buildType(atomicfu)
    buildType(Aggregate_user_projects)
    buildType(kotlinxCoroutinesMacOS)
    buildType(YouTrack)
    buildType(kotlinpoet)
    buildType(benchmark)
    buildType(ktor)
    buildType(coroutines)
    buildType(Exposed)

    subProject(KotlinxTrain.Project)
})

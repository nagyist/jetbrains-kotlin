package Aligners

import Aligners.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Aligners")
    name = "Aligners"

    buildType(MainAggregateAligner)
    buildType(AlignDivergeCommitSearch)
    buildType(AlignSync)
    buildType(AlignCleanup)
    buildType(AlignPushLatest)
})

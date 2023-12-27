package UserProjectsK2Rebase

import UserProjectsK2Rebase.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("UserProjectsK2Rebase")
    name = "Rebase"

    buildType(RebaseAggregate)
})

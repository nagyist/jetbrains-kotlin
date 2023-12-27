package UserProjectsK2Rebase.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object RebaseAggregate : BuildType({
    name = "Rebase All Projects"

    type = BuildTypeSettings.Type.COMPOSITE
})

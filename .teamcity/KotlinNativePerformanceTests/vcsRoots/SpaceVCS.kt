package KotlinNativePerformanceTests.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object SpaceVCS : GitVcsRoot({
    name = "Space"
    url = "ssh://git.jetbrains.team/space.git"
    branch = "refs/heads/native-performance"
    branchSpec = """
        +:refs/heads/*
        -:ecs/preprod
    """.trimIndent()
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "default teamcity key"
    }
})

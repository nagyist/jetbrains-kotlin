package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object coroutinesForAggregateVCS : GitVcsRoot({
    name = "kotlinx.coroutines"
    url = "git@github.com:Kotlin/kotlinx.coroutines.git"
    branch = "refs/heads/kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})

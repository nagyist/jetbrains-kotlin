package UserProjectCompiling.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object kotlinxbenchmark : GitVcsRoot({
    name = "kotlinx-benchmark"
    url = "https://github.com/Kotlin/kotlinx-benchmark.git"
    branch = "refs/heads/%branch.benchmark%"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

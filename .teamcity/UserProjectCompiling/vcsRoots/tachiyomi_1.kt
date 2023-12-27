package UserProjectCompiling.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object tachiyomi_1 : GitVcsRoot({
    id("tachiyomi")
    name = "tachiyomi"
    url = "ssh://git@git.jetbrains.team/kct/tachiyomi_kct.git"
    branch = "refs/heads/%branch.tachiyomi%"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

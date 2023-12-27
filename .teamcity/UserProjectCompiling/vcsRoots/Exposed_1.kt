package UserProjectCompiling.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object Exposed_1 : GitVcsRoot({
    id("Exposed")
    name = "Exposed"
    url = "ssh://git@git.jetbrains.team/exposed_kct.git"
    branch = "refs/heads/%branch.exposed%"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

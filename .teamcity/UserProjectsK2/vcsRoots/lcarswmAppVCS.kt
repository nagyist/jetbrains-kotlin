package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object lcarswmAppVCS : GitVcsRoot({
    name = "lcarswm.app.k2"
    url = "ssh://git@git.jetbrains.team/kct/lcarswm_kct.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

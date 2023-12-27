package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KaMPKitK2VCS : GitVcsRoot({
    name = "KaMPKit.k2"
    url = "ssh://git@git.jetbrains.team/kct/KaMPKit_mpp.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

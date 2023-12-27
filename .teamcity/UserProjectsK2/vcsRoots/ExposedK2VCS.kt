package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object ExposedK2VCS : GitVcsRoot({
    name = "exposed k2"
    url = "ssh://git@git.jetbrains.team/exposed_kct.git"
    branch = "kotlin-community/k2/community-project-plugin"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

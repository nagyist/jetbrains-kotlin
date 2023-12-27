package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object RealmKotlinK2VCS : GitVcsRoot({
    name = "RealmKotlin.k2"
    url = "ssh://git@git.jetbrains.team/kct/realm-kotlin_mpp.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    checkoutSubmodules = GitVcsRoot.CheckoutSubmodules.IGNORE
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

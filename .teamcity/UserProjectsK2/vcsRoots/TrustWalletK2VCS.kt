package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object TrustWalletK2VCS : GitVcsRoot({
    name = "TrustWallet.k2"
    url = "ssh://git@git.jetbrains.team/kct/wallet-core_mpp.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

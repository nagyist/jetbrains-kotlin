package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object AwsSdkSmithyK2VCS : GitVcsRoot({
    name = "AwsSdkSmithy.k2"
    url = "ssh://git@git.jetbrains.team/kct/aws-sdk-kotlin_mpp.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

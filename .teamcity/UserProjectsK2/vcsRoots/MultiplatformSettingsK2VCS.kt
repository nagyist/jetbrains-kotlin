package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object MultiplatformSettingsK2VCS : GitVcsRoot({
    name = "MultiplatformSettings.k2"
    url = "ssh://git@git.jetbrains.team/kct/multiplatform-settings_mpp.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

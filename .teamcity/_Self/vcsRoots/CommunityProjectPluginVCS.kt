package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object CommunityProjectPluginVCS : GitVcsRoot({
    name = "Community project plugin VCS"
    url = "ssh://git@git.jetbrains.team/kti/community-project-plugin.git"
    branch = "master"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object CompatibleComposeVCS : GitVcsRoot({
    name = "Compatible Compose"
    url = "ssh://git@git.jetbrains.space/kotlin/kotlin/compatible-compose.git"
    branch = "refs/heads/master"
    branchSpec = "+:refs/heads/*"
    authMethod = uploadedKey {
        uploadedKey = "Read_Repositories_in_Kotlin_Project_https___kotlin.jetbrains.space_extensions_installedApplications_Read%20Repositories%20in%20Kotlin%20Project-3e1QT234Bhs6_overview"
    }
})

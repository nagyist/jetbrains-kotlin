package UserProjectCompiling.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object kotlinpoet_1 : GitVcsRoot({
    id("kotlinpoet")
    name = "kotlinpoet"
    url = "ssh://git@git.jetbrains.team/kct/kotlinpoet_kct.git"
    branch = "refs/heads/%branch.kotlinpoet%"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

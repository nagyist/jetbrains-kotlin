package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object SpringPetclinicK2VCS : GitVcsRoot({
    name = "spring.petclinic.k2"
    url = "ssh://git@git.jetbrains.team/kct/spring-petclinic-kotlin.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})

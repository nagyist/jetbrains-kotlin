package KotlinNative.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object Kotlinx_Coroutines : GitVcsRoot({
    name = "kotlinx.coroutines"
    url = "https://github.com/Kotlin/kotlinx.coroutines"
    branch = "refs/heads/native-performance"
    authMethod = password {
        userName = "KotlinBuild"
        password = "credentialsJSON:c1e0b1ac-5267-43ab-954c-d766f2c2421c"
    }
})

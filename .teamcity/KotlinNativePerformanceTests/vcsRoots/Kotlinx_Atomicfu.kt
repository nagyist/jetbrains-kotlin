package KotlinNativePerformanceTests.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object Kotlinx_Atomicfu : GitVcsRoot({
    name = "kotlinx.atomicfu"
    url = "https://github.com/Kotlin/kotlinx.atomicfu"
    branch = "refs/heads/native-performance"
    authMethod = password {
        userName = "KotlinBuild"
        password = "credentialsJSON:c1e0b1ac-5267-43ab-954c-d766f2c2421c"
    }
})

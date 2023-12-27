package KotlinBuildTests

import KotlinBuildTests.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("KotlinBuildTests")
    name = "Kotlin Build Tests"
    description = "Tests for Kotlin Gradle build (build cache, publications, etc...)"

    buildType(TestBuildingKotlinWithCache_Windows)
    buildType(BuildKotlinToDirectoryCache)
    buildType(TestBuildingKotlinWithCache_Linux)
    buildType(TestBuildingKotlinWithCache_Linux_RemoteCache)
    buildType(AllKotlinBuildTests)
})

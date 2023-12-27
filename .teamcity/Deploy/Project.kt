package Deploy

import Deploy.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Deploy")
    name = "Deploy"

    buildType(DeployCompilerArtifactsToGitHub)
    buildType(PublishToNpm)
    buildType(KotlinNativePublishToS3)
    buildType(CreateSonatypeStagingRepository)
    buildType(DeployMavenArtifacts)
    buildType(PublishMavenArtifactsToGitHub)
    buildType(KotlinNativePublish)
    buildType(PublishToGradlePluginPortalValidate)
    buildType(DeployCompose)
    buildType(DeployReleasePageDraftToGitHub)
    buildType(DeployMavenArtifactsSonatypeSnapshot)
    buildType(DeployKotlinMavenArtifacts)
    buildType(DeployGradlePluginIdea)
    buildType(DeployIdePluginDependenciesMavenArtifacts)
    buildType(KotlinNativePublishMaven)
    buildType(DeployMavenArtifacts_Nightly_kotlin_space_packages)
    buildType(RelayFromSpaceToSonatype)
    buildType(RelayNightlyDevToSonatype)
    buildTypesOrder = arrayListOf(DeployMavenArtifacts, DeployCompilerArtifactsToGitHub, DeployReleasePageDraftToGitHub, PublishMavenArtifactsToGitHub, DeployKotlinMavenArtifacts, DeployMavenArtifactsSonatypeSnapshot, DeployMavenArtifacts_Nightly_kotlin_space_packages, PublishToNpm, DeployIdePluginDependenciesMavenArtifacts, DeployGradlePluginIdea, PublishToGradlePluginPortalValidate, DeployCompose, RelayNightlyDevToSonatype, RelayFromSpaceToSonatype, KotlinNativePublish, KotlinNativePublishMaven, KotlinNativePublishToS3, CreateSonatypeStagingRepository)
})

package KotlinBuildTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*

object AllKotlinBuildTests : BuildType({
    name = "All Kotlin build tests"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%build.number.default%"

    params {
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    dependencies {
        snapshot(BuildKotlinToDirectoryCache) {
        }
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(TestBuildingKotlinWithCache_Linux) {
        }
        snapshot(TestBuildingKotlinWithCache_Linux_RemoteCache) {
        }
        snapshot(TestBuildingKotlinWithCache_Windows) {
        }
    }
})

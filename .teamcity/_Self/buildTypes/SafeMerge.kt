package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object SafeMerge : BuildType({
    name = "Safe-Merge"
    description = "Composite build for Safe-Merge, started from space Merge Request"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("teamcity.ui.runButton.caption", "")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = "+:<default>"
        }
    }

    dependencies {
        snapshot(Tests_Linux.buildTypes.CodebaseTests) {
        }
        snapshot(Tests_Linux.buildTypes.GenerateTests_LINUX) {
        }
    }
})

package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object IdePluginsAggregate : BuildType({
    name = "IDE Plugins Aggregate"
    description = "Check https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/docs/cooperative-development/environment-setup.md for local investigation"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 300
            branchFilter = """
                +:<default>
                +:rr/ide/*
            """.trimIndent()
        }
    }

    dependencies {
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(AbsoluteId("ijplatform_master_KotlinKtBranchAggregatorLatestCompiler_master")) {
        }
    }
})

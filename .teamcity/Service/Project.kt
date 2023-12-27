package Service

import Service.buildTypes.*
import Service.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Service")
    name = "Service"

    vcsRoot(AgentsConfigurationAssigner_1)
    vcsRoot(BuildFailureAnalyzer_1)

    buildType(AgentsConfigurationAssigner)
    buildType(SynchronizeMutedTestsLINUX)
    buildType(BuildFailureAnalyzer)
    buildType(CompilerDiagnosticsIntroductionNotifier)
    buildType(AutoPush)
    buildType(BuildNumberTag)
})

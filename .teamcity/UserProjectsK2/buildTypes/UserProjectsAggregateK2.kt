package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object UserProjectsAggregateK2 : BuildType({
    name = "K2 All Projects"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

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
        snapshot(AnkiAndroidK2) {
        }
        snapshot(ArrowK2) {
        }
        snapshot(AwsSdkSmithyK2) {
        }
        snapshot(DokkaK2) {
        }
        snapshot(ExposedK2) {
        }
        snapshot(FirebaseK2) {
        }
        snapshot(FlystoK2) {
        }
        snapshot(IntellijK2) {
        }
        snapshot(KaMPKitK2) {
        }
        snapshot(KmShopK2) {
        }
        snapshot(KmmSampleK2) {
        }
        snapshot(KotlinPoetK2) {
        }
        snapshot(KotlinxAtomicFuK2) {
        }
        snapshot(KotlinxBenchmarkK2) {
        }
        snapshot(KotlinxCoroutinesK2) {
        }
        snapshot(KotlinxDateTimeK2) {
        }
        snapshot(KspPlaygroundK2) {
        }
        snapshot(KtorK2) {
        }
        snapshot(LcarswmAppK2) {
        }
        snapshot(MokoResourcesK2) {
        }
        snapshot(MultiplatformSettingsK2) {
        }
        snapshot(NowinandroidK2) {
        }
        snapshot(PeopleInSpaceK2) {
        }
        snapshot(ReaktiveMppK2) {
        }
        snapshot(RealmKotlinK2) {
        }
        snapshot(SpaceK2) {
        }
        snapshot(SpaceiOSK2) {
        }
        snapshot(SphinxK2) {
        }
        snapshot(SpringPetclinicK2) {
        }
        snapshot(TrustWalletK2) {
        }
    }
})

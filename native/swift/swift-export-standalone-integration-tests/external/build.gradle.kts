plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "A set of integration tests for Swift Export Standalone based on external projects"

dependencies {
    compileOnly(kotlinStdlib())

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(project(":native:swift:swift-export-standalone-integration-tests"))
    testImplementation(project(":native:external-projects-test-utils"))
    testRuntimeOnly(projectTests(":analysis:low-level-api-fir"))
    testRuntimeOnly(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val test by nativeTestWithExternalDependencies("test", requirePlatformLibs = true) {
    dependsOn(":kotlin-native:distInvalidateStaleCaches")
}

testsJar()

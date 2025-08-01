plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Printer for SIR"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

    implementation(project(":core:util.runtime"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val testDataDir = projectDir.resolve("testData")

projectTest(jUnitMode = JUnitMode.JUnit5) {
    inputs.dir(testDataDir)
    useJUnitPlatform { }
}

testsJar()

publish()

runtimeJar()
sourcesJar()
javadocJar()

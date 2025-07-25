/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.junit.Test
import kotlin.test.*

internal class CompilationSpecificPluginPath {
    @Test
    fun `native common sourceset should be compiled with common plugins`() {
        // Given plugin applicable to all platforms
        class CommonPlugin : FakeSubPlugin("common", { true })

        val project = buildProject {
            project.plugins.apply("kotlin-multiplatform")

            plugins.apply(CommonPlugin::class.java)

            kotlin {

                linuxX64()
                mingwX64()
                jvm()

                sourceSets.apply {
                    val commonMain = getByName("commonMain")
                    val nativeMain = create("nativeMain")
                    val linuxX64 = getByName("linuxX64Main")
                    val mingwX64 = getByName("mingwX64Main")
                    val jvm = getByName("jvmMain")

                    // Make nativeMain be common source set for linuxX64 and mingwX64
                    nativeMain.dependsOn(commonMain)
                    linuxX64.dependsOn(nativeMain)
                    mingwX64.dependsOn(nativeMain)
                    jvm.dependsOn(commonMain)
                }
            }
        }
        project.evaluate()

        // Then expect common artifact to be used for all compilations, including nativeMain metadata
        assertEquals(setOf("common"), project.compileTaskSubplugins("compileNativeMainKotlinMetadata"))
        assertEquals(setOf("common"), project.compileTaskSubplugins("compileKotlinMetadata"))
        assertEquals(setOf("common"), project.compileTaskSubplugins("compileCommonMainKotlinMetadata"))
        assertEquals(setOf("common"), project.compileTaskSubplugins("compileKotlinJvm"))
        assertEquals(setOf("common"), project.compileTaskSubplugins("compileKotlinLinuxX64"))
    }

    @Test
    fun `each compilation should have its own plugin classpath`() {
        val project = buildProjectWithMPP {
            kotlin {
                val jvmAttribute = Attribute.of(String::class.java)
                jvm("jvm1") { attributes { attribute(jvmAttribute, "jvm1") } }
                jvm("jvm2") { attributes { attribute(jvmAttribute, "jvm2") } }
                js("js") { browser() }
            }
        }

        // Expect to see plugin classpath for each target compilation
        listOf("jvm1", "jvm2", "js")
            .map(String::capitalizeAsciiOnly)
            .flatMap { listOf(pluginClassPathConfiguration(it, "main"), pluginClassPathConfiguration(it, "test")) }
            .plus(pluginClassPathConfiguration("metadata", "main")) // and also one for metadata
            .forEach { assertNotNull(project.configurations.findByName(it), "Configuration $it should exist") }
    }

    @Test
    fun `kotlin plugin author can control plugin artifacts per compilation`() {
        // Given platform-specific Kotlin plugins
        class JvmOnly : FakeSubPlugin("jvmOnly", { target.platformType == KotlinPlatformType.jvm })
        class JsOnly : FakeSubPlugin("jsOnly", { target.platformType == KotlinPlatformType.js })

        // When apply them to a Kotlin MPP Project
        val project = buildProjectWithMPP {
            plugins.apply(JvmOnly::class.java)
            plugins.apply(JsOnly::class.java)

            kotlin {
                jvm("desktop")
                js("web", IR) {
                    browser()
                }
            }
        }
        project.evaluate()

        // Then each plugin classpath should have its own dependency
        assertEquals(listOf("jvmOnly"), project.subplugins("desktop"))
        assertEquals(listOf("jsOnly"), project.subplugins("web"))

        // And each compilation task should have its own plugin classpath
        val compileDesktop = project.tasks.getByName("compileKotlinDesktop") as KotlinCompile
        val expectedConfig = project.configurations.getByName(pluginClassPathConfiguration("desktop", "main"))
        assertEquals(expectedConfig, compileDesktop.pluginClasspath.from.single())
    }

    @Test
    fun `only common artifacts should be taken for native platforms`() {
        // Given plugin applicable to all platforms
        class CommonPlugin1 : FakeSubPlugin("common1", { true })

        // And another plugin applicable to all platforms
        class CommonPlugin2 : FakeSubPlugin("common2", { true })

        // When applying these plugins
        val project = buildProjectWithMPP {
            plugins.apply(CommonPlugin1::class.java)
            plugins.apply(CommonPlugin2::class.java)

            kotlin {
                jvm()
                linuxX64()
            }
        }
        project.evaluate()

        // Expect jvm and native targets have both common artifacts
        assertEquals(listOf("common1", "common2"), project.subplugins("jvm"))
        assertEquals(listOf("common1", "common2"), project.subplugins("linuxX64"))
    }

    @Test
    fun `native plugin configuration should not be transitive`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64()
            }
        }

        val nativeConfig = project
            .configurations
            .getByName(pluginClassPathConfiguration("linuxX64", "main"))

        assertFalse(nativeConfig.isTransitive)
    }

    @Test
    fun `it should be possible to add common plugin classpath to all compilations except native`() {
        // Given MPP project
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                js { browser() }
                linuxX64("native")
            }
        }

        // When adding a shared plugin to common Plugin Classpath
        project.dependencies.add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, "test:shared")
        project.evaluate()

        // Then ALL compilations should have shared plugin EXCEPT native
        assertTrue("shared" in project.subplugins("jvm"))
        assertTrue("shared" in project.subplugins("js"))
        assertTrue("shared" in project.subplugins("js", "test"))
        assertTrue("shared" in project.subplugins("metadata"))
        assertTrue("shared" !in project.subplugins("native"))
    }

    @Test
    fun `native compilations should have its own shared plugin classpath`() {
        // Given MPP project
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64("linux")
                macosX64("mac")
            }
        }

        // When adding a shared plugin to native Plugin Classpath
        project.dependencies.add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, "test:shared")
        project.evaluate()

        // Then ALL native compilations should have shared plugin
        assertTrue("shared" in project.subplugins("linux"))
        assertTrue("shared" in project.subplugins("mac"))
        assertTrue("shared" in project.subplugins("mac", "test"))

        // And all non-native should not have it
        assertTrue("shared" !in project.subplugins("metadata"))
        assertTrue("shared" !in project.subplugins("jvm"))
    }

    private abstract class FakeSubPlugin(
        val id: String,
        val isApplicablePredicate: KotlinCompilation<*>.() -> Boolean
    ) : KotlinCompilerPluginSupportPlugin {
        override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = kotlinCompilation.isApplicablePredicate()

        override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
            kotlinCompilation.target.project.provider { emptyList() }

        override fun getCompilerPluginId(): String = id

        override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
            "test",
            id
        )
    }

    private fun pluginClassPathConfiguration(target: String, compilation: String) =
        "kotlinCompilerPluginClasspath${target.capitalizeAsciiOnly()}${compilation.capitalizeAsciiOnly()}"

    private fun Project.subplugins(target: String, compilation: String = "main") = this
        .configurations
        .getByName(pluginClassPathConfiguration(target, compilation))
        .allDependencies
        .map { it.name }
        .minus("kotlin-scripting-compiler-embeddable")

    private fun Project.compileTaskSubplugins(taskName: String) = this
        .tasks
        .getByName(taskName)
        .let {
            when (it) {
                is AbstractKotlinNativeCompile<*, *> -> it.compilerPluginClasspath
                is AbstractKotlinCompile<*> -> it.pluginClasspath.from.single()
                else -> error("Unexpected task type with name $taskName. Is it kotlin compile task?")
            }
        }
        ?.let { it as Configuration }
        ?.allDependencies
        ?.map { it.name }
        ?.minus("kotlin-scripting-compiler-embeddable")
        ?.toSet()
        ?: emptySet()

}
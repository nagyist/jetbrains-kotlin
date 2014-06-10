/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.context.GlobalContext
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import org.jetbrains.jet.context.GlobalContextImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactoryService
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver
import org.jetbrains.jet.lang.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.jet.utils.keysToMap
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.java.new.JvmAnalyzerFacade
import org.jetbrains.jet.lang.resolve.java.new.JvmPlatformParameters
import org.jetbrains.jet.lang.resolve.java.new.JvmAnalyzer
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl

fun createMappingForProject(
        globalContext: GlobalContext,
        project: Project,
        analyzerFacade: JvmAnalyzerFacade,
        filesByScopeProvider: (GlobalSearchScope) -> Collection<JetFile>
): ModuleSetup {

    val modules = ModuleManager.getInstance(project).getSortedModules().toList()
    val moduleDependencies = {
        (module: Module) ->
        ModuleRootManager.getInstance(module).getOrderEntries().map {
            orderEntry ->
            when (orderEntry) {
                is ModuleSourceOrderEntry -> { orderEntry.getRootModel().getModule() }
                is ModuleOrderEntry -> { orderEntry.getModule() }
                is LibraryOrderEntry -> {
                    //TODO:
                    val library = orderEntry.getLibrary()!!
                    val isKotlinRuntime = library.getName() == "KotlinJavaRuntime"
                    if (isKotlinRuntime) {
                    }
                    null
                }
                else -> {
                    null
                }
            }
        }.filterNotNull()
    }
    val jvmPlatformParameters = {(module: Module) ->
        JvmPlatformParameters(filesByScopeProvider(GlobalSearchScope.moduleScope(module)), GlobalSearchScope.moduleScope(module)) {
            javaClass ->
            ModuleUtilCore.findModuleForPsiElement((javaClass as JavaClassImpl).getPsi())!!
        }
    }
    val analysisSetup = analyzerFacade.setupAnalysis(
            globalContext, project, modules, moduleDependencies, ::constructModuleName, jvmPlatformParameters
    )

    val moduleToBodiesResolveSession = modules.keysToMap {
        module ->
        val descriptor = analysisSetup.descriptorByModule[module]!!
        val analyzer = analysisSetup.analyzerByModuleDescriptor[descriptor]!!
        ResolveSessionForBodies(project, analyzer.lazyResolveSession)
    }
    return ModuleSetup(analysisSetup.descriptorByModule, analysisSetup.analyzerByModuleDescriptor, moduleToBodiesResolveSession)
}

//TODO: actually nullable
class ModuleSetup(val descriptorByModule: Map<Module, ModuleDescriptor>,
                  val setupByModuleDescriptor: Map<ModuleDescriptor, JvmAnalyzer>,
                  val bodiesResolveByModule: Map<Module, ResolveSessionForBodies>
) {
    fun descriptorByModule(module: Module) = descriptorByModule[module]!!
    fun setupByModule(module: Module) = setupByModuleDescriptor[descriptorByModule[module]!!]!!
    fun resolveSessionForBodiesByModule(module: Module) = bodiesResolveByModule[module]!!
    val modules: Collection<Module> = descriptorByModule.keySet()
}


private fun constructModuleName(module: Module): Name {
    return Name.special("<${module.getName()}>")
}


private fun createScopeRestrictedDPF(files: Collection<JetFile>, globalContext: GlobalContextImpl, project: Project, scope: GlobalSearchScope): DeclarationProviderFactory {
    files.filter { it.getOriginalFile().getVirtualFile()?.let { scope.contains(it) } ?: false }
    return DeclarationProviderFactoryService.createDeclarationProviderFactory(project, globalContext.storageManager, files)
}

class PackageFragmentProviderForSource(val resolveSession: ResolveSession, val javaDescriptorResolver: JavaDescriptorResolver)
: CompositePackageFragmentProvider(listOf(resolveSession.getPackageFragmentProvider(), javaDescriptorResolver.getPackageFragmentProvider()))

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

package org.jetbrains.jet.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.module.ModuleManager
import com.intellij.psi.PsiManager
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.jet.plugin.JetPluginUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheService
import org.jetbrains.jet.lang.diagnostics.Severity
import java.util.ArrayList
import java.io.StringWriter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.ui.Messages
import java.io.PrintWriter
import org.jetbrains.jet.utils.*
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.fileEditor.FileEditorManager

public class CheckAllKotlinFiles : AnAction() {



    override fun actionPerformed(e: AnActionEvent?) {
        val project = CommonDataKeys.PROJECT.getData(e!!.getDataContext())
        if (project == null) return

        val modules = ModuleManager.getInstance(project)!!.getModules()

        val all = ArrayList<JetFile>()
        for (module in modules) {
            val index = ModuleRootManager.getInstance(module)!!.getFileIndex()
            index.iterateContent @f {
                (file): Boolean ->
                if (file!!.isDirectory()) return@f true
                if (!index.isInSourceContent(file) && !index.isInTestSourceContent(file))return@f true
                if (JetPluginUtil.isKtFileInGradleProjectInWrongFolder(file, project)) return@f true

                val fileType = FileTypeManager.getInstance()!!.getFileTypeByFile(file)
                if (fileType != JetFileType.INSTANCE) return@f true
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile is JetFile) {
                    all.add(psiFile)
                }
                return@f true
            }
        }

        object : Task.Backgroundable(project, "Analyzing all Kotlin files") {
            override fun run(indicator: ProgressIndicator) {
                val virtualFile = ApplicationManager.getApplication()?.runReadAction(Computable {
                    val sb = StringWriter()
                    val exhaust = KotlinCacheService.getInstance(project).getAnalysisResults(all)

                    if (exhaust.isError()) {
                        sb.append("Error:\n")
                        exhaust.getError().printStackTrace(PrintWriter(sb))
                    }
                    else {
                        exhaust.getBindingContext().getDiagnostics()
                                .toSet()
                                .filter { it.getSeverity() == Severity.ERROR }
                                .ifEmpty {
                                    sb.append("No errors")
                                    listOf()
                                }
                                .forEach {
                                    sb.append("" + it.getFactory() + " in " + it.getPsiElement().getContainingFile()).append("\n")
                                }
                    }


                    val file = FileUtil.createTempFile("errors", ".txt")
                    file.writeText(sb.toString())

                    val vFile = LocalFileSystem.getInstance()?.findFileByIoFile(file)
                    if (vFile == null) {
                        Messages.showMessageDialog("Can't find file: $file", "Error opening file", null)
                    }
                    vFile
                })

                if (virtualFile != null) {
                    ApplicationManager.getApplication()?.invokeLater {
                        ApplicationManager.getApplication()?.runWriteAction {
                            FileEditorManager.getInstance(project)?.openFile(virtualFile, false)
                        }
                    }
                }
            }
        }.queue()

    }
}
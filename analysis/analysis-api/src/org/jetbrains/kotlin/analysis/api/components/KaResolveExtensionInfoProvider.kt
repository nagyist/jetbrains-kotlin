/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.psi.KtElement

@KaExperimentalApi
public interface KaResolveExtensionInfoProvider {
    /**
     * Returns [KaScope] which contains all top-level callable declarations which are generated by [KaResolveExtension]
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
     */
    @KaExperimentalApi
    public val resolveExtensionScopeWithTopLevelDeclarations: KaScope

    /**
     * Returns whether this [VirtualFile] was provided by a [KaResolveExtension].
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
     */
    @KaExperimentalApi
    public val VirtualFile.isResolveExtensionFile: Boolean

    /**
     * Returns whether this [KtElement] was provided by a [KaResolveExtension].
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
     */
    @KaExperimentalApi
    public val KtElement.isFromResolveExtension: Boolean

    /**
     * Returns the [PsiElement]s which should be used as a navigation target in place of this [KtElement]
     * provided by a [KaResolveExtension].
     *
     * These [PsiElement]s will typically be the source item(s) that caused the given [KtElement] to be generated
     * by the [KaResolveExtension]. For example, for a [KtElement] generated by a resource compiler, this will
     * typically be a list of the [PsiElement]s of the resource items in the corresponding resource file.
     *
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
     * @see org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
     */
    @KaExperimentalApi
    public val KtElement.resolveExtensionNavigationElements: Collection<PsiElement>
}
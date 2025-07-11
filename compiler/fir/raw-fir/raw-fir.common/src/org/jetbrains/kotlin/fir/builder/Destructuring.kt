/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

interface DestructuringContext<T> {
    val T.returnTypeRef: FirTypeRef
    val T.name: Name
    val T.source: KtSourceElement
    fun T.extractAnnotationsTo(target: FirAnnotationContainerBuilder, containerSymbol: FirBasedSymbol<*>)
    fun createComponentCall(container: FirVariable, entrySource: KtSourceElement?, index: Int): FirExpression {
        return container.toComponentCall(entrySource, index)
    }
}

fun <T> AbstractRawFirBuilder<*>.addDestructuringVariables(
    destination: MutableList<in FirVariable>,
    c: DestructuringContext<T>,
    moduleData: FirModuleData,
    container: FirVariable,
    entries: List<T>,
    isVar: Boolean,
    tmpVariable: Boolean,
    forceLocal: Boolean,
    configure: (FirVariable) -> Unit = {}
) {
    if (tmpVariable) {
        destination += container
    }
    for ((index, entry) in entries.withIndex()) {
        destination += buildDestructuringVariable(
            moduleData,
            c,
            container,
            entry,
            isVar,
            forceLocal,
            index,
            configure,
        )
    }
}

fun <T> AbstractRawFirBuilder<*>.buildDestructuringVariable(
    moduleData: FirModuleData,
    c: DestructuringContext<T>,
    container: FirVariable,
    entry: T,
    isVar: Boolean,
    forceLocal: Boolean,
    index: Int,
    configure: (FirVariable) -> Unit = {}
): FirVariable = with(c) {
    buildProperty {
        val localEntries = forceLocal || context.inLocalContext
        symbol = when {
            localEntries -> FirLocalPropertySymbol()
            else -> FirRegularPropertySymbol(callableIdForName(entry.name))
        }
        withContainerSymbol(symbol, localEntries) {
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = entry.returnTypeRef
            name = entry.name
            initializer = createComponentCall(container, entry.source, index)
            this.isVar = isVar
            source = entry.source
            status = FirDeclarationStatusImpl(if (localEntries) Visibilities.Local else Visibilities.Public, Modality.FINAL)
            entry.extractAnnotationsTo(this, context.containerSymbol)
            if (!localEntries) {
                getter = FirDefaultPropertyGetter(
                    source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                    moduleData = moduleData,
                    origin = FirDeclarationOrigin.Source,
                    propertyTypeRef = returnTypeRef,
                    visibility = Visibilities.Public,
                    propertySymbol = symbol,
                    modality = Modality.FINAL,
                )
                if (isVar) {
                    setter = FirDefaultPropertySetter(
                        source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                        moduleData = moduleData,
                        origin = FirDeclarationOrigin.Source,
                        propertyTypeRef = returnTypeRef,
                        visibility = Visibilities.Public,
                        propertySymbol = symbol,
                        modality = Modality.FINAL,
                    )
                }
            }
        }
    }.also(configure)
}

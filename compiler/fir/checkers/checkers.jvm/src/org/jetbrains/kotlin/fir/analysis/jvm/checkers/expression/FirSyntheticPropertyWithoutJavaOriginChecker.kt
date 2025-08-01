/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.noJavaOrigin

object FirSyntheticPropertyWithoutJavaOriginChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        if (LanguageFeature.DontCreateSyntheticPropertiesWithoutBaseJavaGetter.isEnabled()) return
        val syntheticProperty = expression.toResolvedCallableSymbol() as? FirSimpleSyntheticPropertySymbol ?: return
        val containingAssignment = context.callsOrAssignments.getOrNull(context.callsOrAssignments.size - 2) as? FirVariableAssignment
        val isAssignment = containingAssignment?.lValue === expression
        val originalFunction = when (isAssignment) {
            false -> syntheticProperty.getterSymbol?.delegateFunctionSymbol
            true -> syntheticProperty.setterSymbol?.delegateFunctionSymbol
        } ?: return
        if (syntheticProperty.noJavaOrigin) {
            if (LanguageFeature.ForbidSyntheticPropertiesWithoutBaseJavaGetter.isEnabled()) {
                reporter.reportOn(
                    expression.source,
                    FirErrors.FUNCTION_CALL_EXPECTED,
                    originalFunction.name.asString(),
                    false
                )
            } else {
                reporter.reportOn(
                    expression.source,
                    FirJvmErrors.SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN,
                    originalFunction,
                    originalFunction.name
                )
            }
        }
    }
}

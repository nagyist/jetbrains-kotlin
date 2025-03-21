/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.FirCallsEffectAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.FirPropertyInitializationAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirModifierChecker,
            FirConflictsDeclarationChecker,
            FirTypeConstraintsChecker,
            FirReservedUnderscoreDeclarationChecker,
            FirUpperBoundViolatedDeclarationChecker,
            FirExposedVisibilityDeclarationChecker,
            FirCyclicTypeBoundsChecker,
            FirExpectActualDeclarationChecker,
            FirExpectRefinementChecker,
            FirRequiresOptInOnExpectChecker,
            FirAmbiguousAnonymousTypeChecker,
            FirExplicitApiDeclarationChecker,
            FirAnnotationChecker,
            FirPublishedApiChecker,
            FirContextReceiversDeprecatedDeclarationChecker,
            FirOptInMarkedDeclarationChecker,
            FirExpectConsistencyChecker,
            FirOptionalExpectationDeclarationChecker,
            FirMissingDependencySupertypeInDeclarationsChecker,
            FirContextParametersDeclarationChecker,
            FirUnusedReturnValueChecker,
            FirReturnValueAnnotationsChecker,
        )

    override val classLikeCheckers: Set<FirClassLikeChecker>
        get() = setOf(
            FirExpectActualClassifiersAreInBetaChecker,
        )

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker>
        get() = setOf(
            FirKClassWithIncorrectTypeArgumentChecker,
            FirImplicitNothingReturnTypeChecker,
            FirDynamicReceiverChecker,
            FirExtensionShadowedByMemberChecker.Regular,
            FirExtensionShadowedByMemberChecker.ForExpectDeclaration,
        )

    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
            FirContractChecker,
            FirFunctionParameterChecker,
            FirFunctionReturnChecker,
            FirInlineDeclarationChecker,
            FirNonMemberFunctionsChecker,
            FirSuspendLimitationsChecker,
            FirInfixFunctionDeclarationChecker,
            FirOperatorModifierChecker,
            FirTailrecFunctionChecker,
        )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
        get() = setOf(
            FirFunctionNameChecker,
            FirFunctionTypeParametersSyntaxChecker,
            FirMemberFunctionsChecker,
            FirDataObjectContentChecker,
            ContractSyntaxV2FunctionChecker,
            FirAnyDeprecationChecker,
        )

    override val propertyCheckers: Set<FirPropertyChecker>
        get() = setOf(
            FirInapplicableLateinitChecker,
            FirDestructuringDeclarationChecker,
            FirConstPropertyChecker,
            FirPropertyAccessorsTypesChecker,
            FirPropertyTypeParametersChecker,
            FirInitializerTypeMismatchChecker,
            FirDelegatedPropertyChecker,
            FirPropertyFieldTypeChecker,
            FirPropertyFromParameterChecker,
            FirLocalVariableTypeParametersSyntaxChecker,
            FirDelegateUsesExtensionPropertyTypeParameterChecker,
            FirLocalExtensionPropertyChecker,
            ContractSyntaxV2PropertyChecker,
            FirVolatileAnnotationChecker,
            FirInlinePropertyChecker,
            FirUnnamedPropertyChecker,
            FirContextualPropertyWithBackingFieldChecker
        )

    override val backingFieldCheckers: Set<FirBackingFieldChecker>
        get() = setOf(
            FirExplicitBackingFieldForbiddenChecker,
            FirExplicitBackingFieldsUnsupportedChecker,
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirOverrideChecker.Regular,
            FirOverrideChecker.ForExpectClass,
            FirNotImplementedOverrideChecker,
            FirNotImplementedOverrideSimpleEnumEntryChecker.Regular,
            FirNotImplementedOverrideSimpleEnumEntryChecker.ForExpectClass,
            FirThrowableSubclassChecker,
            FirOpenMemberChecker,
            FirClassVarianceChecker,
            FirSealedSupertypeChecker,
            FirMemberPropertiesChecker,
            FirImplementationMismatchChecker.Regular,
            FirImplementationMismatchChecker.ForExpectClass,
            FirTypeParametersInObjectChecker,
            FirSupertypesChecker,
            FirPrimaryConstructorSuperTypeChecker,
            FirDynamicSupertypeChecker,
            FirDataClassConsistentDataCopyAnnotationChecker,
            FirEnumCompanionInEnumConstructorCallChecker,
            FirBadInheritedJavaSignaturesChecker,
            FirSealedInterfaceAllowedChecker,
            FirMixedFunctionalTypesInSupertypesChecker.Regular,
            FirMixedFunctionalTypesInSupertypesChecker.ForExpectClass,
        )

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = setOf(
            FirAnnotationClassDeclarationChecker,
            FirOptInAnnotationClassChecker,
            FirCommonConstructorDelegationIssuesChecker,
            FirDelegationSuperCallInEnumConstructorChecker,
            FirDelegationInExpectClassSyntaxChecker,
            FirDelegationInInterfaceSyntaxChecker,
            FirEnumClassSimpleChecker,
            FirLocalEntityNotAllowedChecker,
            FirManyCompanionObjectsChecker,
            FirMethodOfAnyImplementedInInterfaceChecker,
            FirDataClassPrimaryConstructorChecker,
            FirDataClassNonPublicConstructorChecker,
            FirFunInterfaceDeclarationChecker.Regular,
            FirFunInterfaceDeclarationChecker.ForExpectClass,
            FirNestedClassChecker,
            FirValueClassDeclarationChecker.Regular,
            FirValueClassDeclarationChecker.ForExpectClass,
            FirOuterClassArgumentsRequiredChecker,
            FirPropertyInitializationChecker,
            FirDelegateFieldTypeMismatchChecker,
            FirMultipleDefaultsInheritedFromSupertypesChecker.Regular,
            FirMultipleDefaultsInheritedFromSupertypesChecker.ForExpectClass,
            FirFiniteBoundRestrictionChecker,
            FirNonExpansiveInheritanceRestrictionChecker,
            FirObjectConstructorChecker,
            FirInlineClassDeclarationChecker,
            FirEnumEntryInitializationChecker,
        )

    override val constructorCheckers: Set<FirConstructorChecker>
        get() = setOf(
            FirConstructorAllowedChecker,
            FirMissingConstructorKeywordSyntaxChecker,
        )

    override val fileCheckers: Set<FirFileChecker>
        get() = setOf(
            FirImportsChecker,
            FirOptInImportsChecker,
            FirUnresolvedInMiddleOfImportChecker,
            FirTopLevelPropertiesChecker,
            FirPackageConflictsWithClassifierChecker,
        )

    override val scriptCheckers: Set<FirScriptChecker>
        get() = setOf(
            FirScriptPropertiesChecker
        )

    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker>
        get() = setOf(
            FirCallsEffectAnalyzer,
        )

    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker>
        get() = setOf(
            FirPropertyInitializationAnalyzer,
        )

    override val typeParameterCheckers: Set<FirTypeParameterChecker>
        get() = setOf(
            FirTypeParameterBoundsChecker.Regular,
            FirTypeParameterBoundsChecker.ForExpectClass,
            FirTypeParameterVarianceChecker,
            FirReifiedTypeParameterChecker,
            FirTypeParameterSyntaxChecker,
        )

    override val typeAliasCheckers: Set<FirTypeAliasChecker>
        get() = setOf(
            FirAnyTypeAliasChecker,
            FirActualTypeAliasChecker,
        )

    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker>
        get() = setOf(
            FirAnonymousFunctionParametersChecker,
            FirAnonymousFunctionTypeParametersChecker,
            FirInlinedLambdaNonSourceAnnotationsChecker,
            FirSuspendAnonymousFunctionChecker,
            FirMissingDependencyClassForLambdaReceiverChecker,
        )

    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker>
        get() = setOf(
            FirAnonymousInitializerInInterfaceChecker
        )

    override val valueParameterCheckers: Set<FirValueParameterChecker>
        get() = setOf(
            FirValueParameterDefaultValueTypeMismatchChecker,
            FirMissingDependencyClassForParameterChecker,
        )

    override val enumEntryCheckers: Set<FirEnumEntryChecker>
        get() = setOf(
            FirEnumEntriesRedeclarationChecker,
        )
}

/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.EffectSystem
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.KotlinCallResolver
import org.jetbrains.kotlin.resolve.calls.SPECIAL_FUNCTION_NAMES
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.buildResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallableDescriptors
import org.jetbrains.kotlin.resolve.calls.tasks.OldResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.checkers.PassingProgressionAsCollectionCallChecker
import org.jetbrains.kotlin.resolve.checkers.ResolutionWithStubTypesChecker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isUnderscoreNamed
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.*
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.addToStdlib.compactIfPossible
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class PSICallResolver(
    private val typeResolver: TypeResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
    private val syntheticScopes: SyntheticScopes,
    private val callComponents: KotlinCallComponents,
    private val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
    private val kotlinCallResolver: KotlinCallResolver,
    private val typeApproximator: TypeApproximator,
    private val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val effectSystem: EffectSystem,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val candidateInterceptor: CandidateInterceptor,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val resultTypeResolver: ResultTypeResolver,
) {
    private val callCheckersWithAdditionalResolve = listOf(
        PassingProgressionAsCollectionCallChecker(kotlinCallResolver),
        ResolutionWithStubTypesChecker(kotlinCallResolver)
    )

    private val givenCandidatesName = Name.special("<given candidates>")

    private val arePartiallySpecifiedTypeArgumentsEnabled = languageVersionSettings.supportsFeature(LanguageFeature.PartiallySpecifiedTypeArguments)

    val defaultResolutionKinds = setOf(
        NewResolutionOldInference.ResolutionKind.Function,
        NewResolutionOldInference.ResolutionKind.Variable,
        NewResolutionOldInference.ResolutionKind.Invoke,
        NewResolutionOldInference.ResolutionKind.CallableReference
    )

    fun <D : CallableDescriptor> runResolutionAndInference(
        context: BasicCallResolutionContext,
        name: Name,
        resolutionKind: NewResolutionOldInference.ResolutionKind,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        val kotlinCallKind = resolutionKind.toKotlinCallKind()
        val kotlinCall = toKotlinCall(context, kotlinCallKind, context.call, name, tracingStrategy, isSpecialFunction = false)
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)

        val expectedType = calculateExpectedType(context)
        val result = kotlinCallResolver.resolveAndCompleteCall(
            scopeTower, resolutionCallbacks, kotlinCall, expectedType, context.collectAllCandidates
        )

        if (result.isEmpty() && reportAdditionalDiagnosticIfNoCandidates(context, scopeTower, kotlinCallKind, kotlinCall)) {
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        val overloadResolutionResults = convertToOverloadResolutionResults<D>(context, result, tracingStrategy)

        return overloadResolutionResults.also {
            clearCacheForApproximationResults()
            checkCallWithAdditionalResolve(it, scopeTower, resolutionCallbacks, expectedType, context)
        }
    }

    private fun <D : CallableDescriptor> checkCallWithAdditionalResolve(
        overloadResolutionResults: OverloadResolutionResults<D>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext,
    ) {
        for (callChecker in callCheckersWithAdditionalResolve) {
            callChecker.check(overloadResolutionResults, scopeTower, resolutionCallbacks, expectedType, context)
        }
    }

    // actually, `D` is at least FunctionDescriptor, but right now because of CallResolver it isn't possible change upper bound for `D`
    fun <D : CallableDescriptor> runResolutionAndInferenceForGivenCandidates(
        context: BasicCallResolutionContext,
        resolutionCandidates: Collection<OldResolutionCandidate<D>>,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        val dispatchReceiver = resolutionCandidates.firstNotNullOfOrNull { it.dispatchReceiver }

        val isSpecialFunction = resolutionCandidates.any { it.descriptor.name in SPECIAL_FUNCTION_NAMES }
        val kotlinCall = toKotlinCall(
            context, KotlinCallKind.FUNCTION, context.call, givenCandidatesName, tracingStrategy, isSpecialFunction, dispatchReceiver
        )
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)

        val givenCandidates = resolutionCandidates.map {
            GivenCandidate(
                it.descriptor as FunctionDescriptor,
                it.dispatchReceiver?.let { context.transformToReceiverWithSmartCastInfo(it) },
                it.knownTypeParametersResultingSubstitutor
            )
        }

        val result = kotlinCallResolver.resolveAndCompleteGivenCandidates(
            scopeTower, resolutionCallbacks, kotlinCall, calculateExpectedType(context), givenCandidates, context.collectAllCandidates
        )
        val overloadResolutionResults = convertToOverloadResolutionResults<D>(context, result, tracingStrategy)
        return overloadResolutionResults.also {
            clearCacheForApproximationResults()
        }
    }

    private fun clearCacheForApproximationResults() {
        // Mostly, we approximate captured or some other internal types that don't live longer than resolve for a call,
        // so it's quite useless to preserve cache for longer time
        typeApproximator.clearCache()
    }

    private fun createResolutionCallbacks(context: BasicCallResolutionContext) =
        createResolutionCallbacks(context.trace, context.inferenceSession, context)

    fun createResolutionCallbacks(trace: BindingTrace, inferenceSession: InferenceSession, context: BasicCallResolutionContext) =
        KotlinResolutionCallbacksImpl(
            trace, expressionTypingServices, typeApproximator,
            argumentTypeResolver, languageVersionSettings, kotlinToResolvedCallTransformer,
            dataFlowValueFactory, inferenceSession, constantExpressionEvaluator, typeResolver,
            this, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter, callComponents,
            doubleColonExpressionResolver, deprecationResolver, moduleDescriptor, context, missingSupertypesResolver, kotlinCallResolver,
            resultTypeResolver
        )

    private fun calculateExpectedType(context: BasicCallResolutionContext): UnwrappedType? {
        val expectedType = context.expectedType.unwrap()

        return if (context.contextDependency == ContextDependency.DEPENDENT) {
            if (TypeUtils.noExpectedType(expectedType)) null else expectedType
        } else {
            if (expectedType.isError) TypeUtils.NO_EXPECTED_TYPE else expectedType
        }
    }

    fun <D : CallableDescriptor> convertToOverloadResolutionResults(
        context: BasicCallResolutionContext,
        result: CallResolutionResult,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        if (result is AllCandidatesResolutionResult) {
            val resolvedCalls = result.allCandidates.map { (candidate, diagnostics) ->
                val system = candidate.getSystem()
                val resultingSubstitutor =
                    system.asReadOnlyStorage().buildResultingSubstitutor(system as TypeSystemInferenceExtensionContext)

                kotlinToResolvedCallTransformer.transformToResolvedCall<D>(
                    candidate.resolvedCall, null, resultingSubstitutor, diagnostics
                )
            }

            return AllCandidates(resolvedCalls)
        }

        val trace = context.trace

        handleErrorResolutionResult<D>(context, trace, result, tracingStrategy)?.let { errorResult ->
            context.inferenceSession.addErrorCallInfo(PSIErrorCallInfo(result, errorResult))
            return errorResult
        }

        val resolvedCall = kotlinToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

        // NB. Be careful with moving this invocation, as effect system expects resolution results to be written in trace
        // (see EffectSystem for details)
        resolvedCall.recordEffects(trace)

        return SingleOverloadResolutionResult(resolvedCall)
    }

    private fun <D : CallableDescriptor> handleErrorResolutionResult(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        result: CallResolutionResult,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D>? {
        val diagnostics = result.diagnostics

        diagnostics.firstIsInstanceOrNull<NoneCandidatesCallDiagnostic>()?.let {
            kotlinToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

            tracingStrategy.unresolvedReference(trace)
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        diagnostics.firstIsInstanceOrNull<ManyCandidatesCallDiagnostic>()?.let {
            kotlinToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

            return transformManyCandidatesAndRecordTrace(it, tracingStrategy, trace, context)
        }

        if (getResultApplicability(diagnostics.filterErrorDiagnostics()) == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER) {
            val singleCandidate = result.resultCallAtom() ?: error("Should be not null for result: $result")
            val resolvedCall = kotlinToResolvedCallTransformer.onlyTransform<D>(singleCandidate, diagnostics).also {
                tracingStrategy.unresolvedReferenceWrongReceiver(trace, listOf(it))
            }

            return SingleOverloadResolutionResult(resolvedCall)
        }

        return null
    }

    private fun <D : CallableDescriptor> transformManyCandidatesAndRecordTrace(
        diagnostic: ManyCandidatesCallDiagnostic,
        tracingStrategy: TracingStrategy,
        trace: BindingTrace,
        context: BasicCallResolutionContext
    ): ManyCandidates<D> {
        val resolvedCalls = diagnostic.candidates.map {
            kotlinToResolvedCallTransformer.onlyTransform<D>(
                it.resolvedCall, it.diagnostics + it.getSystem().errors.asDiagnostics()
            )
        }

        if (diagnostic.candidates.areAllFailed()) {
            if (diagnostic.candidates.areAllFailedWithInapplicableWrongReceiver()) {
                tracingStrategy.unresolvedReferenceWrongReceiver(trace, resolvedCalls)
            } else {
                tracingStrategy.noneApplicable(trace, resolvedCalls)
                tracingStrategy.recordAmbiguity(trace, resolvedCalls)
            }
        } else {
            tracingStrategy.recordAmbiguity(trace, resolvedCalls)
            if (!context.call.hasUnresolvedArguments(context)) {
                if (resolvedCalls.allIncomplete) {
                    tracingStrategy.cannotCompleteResolve(trace, resolvedCalls)
                } else {
                    tracingStrategy.ambiguity(trace, resolvedCalls)
                }
            }
        }
        return ManyCandidates(resolvedCalls)
    }

    private val List<ResolvedCall<*>>.allIncomplete: Boolean get() = all { it.status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE }

    private fun ResolvedCall<*>.recordEffects(trace: BindingTrace) {
        val moduleDescriptor = DescriptorUtils.getContainingModule(this.resultingDescriptor.containingDeclaration)
        recordLambdasInvocations(trace, moduleDescriptor)
        recordResultInfo(trace, moduleDescriptor)
    }

    private fun ResolvedCall<*>.recordResultInfo(trace: BindingTrace, moduleDescriptor: ModuleDescriptor) {
        if (this !is NewResolvedCallImpl) return
        val resultDFIfromES = effectSystem.getDataFlowInfoForFinishedCall(this, trace, moduleDescriptor)
        this.updateResultingDataFlowInfo(resultDFIfromES)
    }

    private fun ResolvedCall<*>.recordLambdasInvocations(trace: BindingTrace, moduleDescriptor: ModuleDescriptor) {
        effectSystem.recordDefiniteInvocations(this, trace, moduleDescriptor)
    }

    private fun CallResolutionResult.isEmpty(): Boolean =
        diagnostics.firstIsInstanceOrNull<NoneCandidatesCallDiagnostic>() != null

    private fun Collection<ResolutionCandidate>.areAllFailed() = all { !it.isSuccessful }

    private fun Collection<ResolutionCandidate>.areAllFailedWithInapplicableWrongReceiver() =
        all {
            it.resultingApplicability == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
        }

    private fun CallResolutionResult.areAllInapplicable(): Boolean {
        val manyCandidates = diagnostics.firstIsInstanceOrNull<ManyCandidatesCallDiagnostic>()?.candidates
        if (manyCandidates != null) {
            return manyCandidates.areAllFailed()
        }

        val applicability = getResultApplicability(diagnostics)
        return applicability == CandidateApplicability.INAPPLICABLE ||
                applicability == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER ||
                applicability == CandidateApplicability.HIDDEN
    }

    // true if we found something
    private fun reportAdditionalDiagnosticIfNoCandidates(
        context: BasicCallResolutionContext,
        scopeTower: ImplicitScopeTower,
        kind: KotlinCallKind,
        kotlinCall: KotlinCall
    ): Boolean {
        val reference = context.call.calleeExpression as? KtReferenceExpression ?: return false

        val errorCandidates = when (kind) {
            KotlinCallKind.FUNCTION ->
                collectErrorCandidatesForFunction(scopeTower, kotlinCall.name, kotlinCall.explicitReceiver?.receiver)
            KotlinCallKind.VARIABLE ->
                collectErrorCandidatesForVariable(scopeTower, kotlinCall.name, kotlinCall.explicitReceiver?.receiver)
            else -> emptyList()
        }

        for (candidate in errorCandidates) {
            if (candidate is ErrorCandidate.Classifier) {
                context.trace.record(BindingContext.REFERENCE_TARGET, reference, candidate.descriptor)
                context.trace.report(
                    Errors.RESOLUTION_TO_CLASSIFIER.on(
                        reference,
                        candidate.descriptor,
                        candidate.kind,
                        candidate.errorMessage
                    )
                )
                return true
            }
        }
        return false
    }


    private inner class ASTScopeTower(
        val context: BasicCallResolutionContext,
        ktExpression: KtExpression? = null
    ) : ImplicitScopeTower {
        // todo may be for invoke for case variable + invoke we should create separate dynamicScope(by newCall for invoke)
        override val dynamicScope: MemberScope =
            dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)

        // same for location
        override val location: LookupLocation = ktExpression?.createLookupLocation() ?: context.call.createLookupLocation()

        override val syntheticScopes: SyntheticScopes get() = this@PSICallResolver.syntheticScopes
        override val isDebuggerContext: Boolean get() = context.isDebuggerContext
        override val isNewInferenceEnabled: Boolean get() = context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        override val areContextReceiversEnabled: Boolean get() = context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)
        override val languageVersionSettings: LanguageVersionSettings get() = context.languageVersionSettings
        override val lexicalScope: LexicalScope get() = context.scope
        override val typeApproximator: TypeApproximator get() = this@PSICallResolver.typeApproximator
        override val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter get() = this@PSICallResolver.implicitsResolutionFilter
        private val cache = HashMap<ReceiverParameterDescriptor, ReceiverValueWithSmartCastInfo>()

        override fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo? {
            val implicitReceiver = scope.implicitReceiver ?: return null

            return cache.getOrPut(implicitReceiver) {
                context.transformToReceiverWithSmartCastInfo(implicitReceiver.value)
            }
        }

        override fun getContextReceivers(scope: LexicalScope): List<ReceiverValueWithSmartCastInfo> =
            scope.contextReceiversGroup.map { cache.getOrPut(it) { context.transformToReceiverWithSmartCastInfo(it.value) } }

        override fun getNameForGivenImportAlias(name: Name): Name? =
            (context.call.callElement.containingFile as? KtFile)?.getNameForGivenImportAlias(name)

        override fun interceptFunctionCandidates(
            resolutionScope: ResolutionScope,
            name: Name,
            initialResults: Collection<FunctionDescriptor>,
            location: LookupLocation,
            dispatchReceiver: ReceiverValueWithSmartCastInfo?,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): Collection<FunctionDescriptor> {
            return candidateInterceptor.interceptFunctionCandidates(
                initialResults,
                this,
                context,
                resolutionScope,
                this@PSICallResolver,
                name,
                location,
                dispatchReceiver,
                extensionReceiver
            )
        }

        override fun interceptVariableCandidates(
            resolutionScope: ResolutionScope,
            name: Name,
            initialResults: Collection<VariableDescriptor>,
            location: LookupLocation,
            dispatchReceiver: ReceiverValueWithSmartCastInfo?,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): Collection<VariableDescriptor> {
            return candidateInterceptor.interceptVariableCandidates(
                initialResults,
                this,
                context,
                resolutionScope,
                this@PSICallResolver,
                name,
                location,
                dispatchReceiver,
                extensionReceiver
            )
        }
    }

    inner class FactoryProviderForInvoke(
        val context: BasicCallResolutionContext,
        val scopeTower: ImplicitScopeTower,
        val kotlinCall: PSIKotlinCallImpl
    ) : CandidateFactoryProviderForInvoke<ResolutionCandidate> {

        init {
            assert(kotlinCall.dispatchReceiverForInvokeExtension == null) { kotlinCall }
        }

        override fun transformCandidate(
            variable: ResolutionCandidate,
            invoke: ResolutionCandidate
        ) = invoke

        override fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<ResolutionCandidate> {
            val explicitReceiver = if (stripExplicitReceiver) null else kotlinCall.explicitReceiver
            val variableCall = PSIKotlinCallForVariable(kotlinCall, explicitReceiver, kotlinCall.name)
            return SimpleCandidateFactory(callComponents, scopeTower, variableCall, createResolutionCallbacks(context))
        }

        override fun factoryForInvoke(variable: ResolutionCandidate, useExplicitReceiver: Boolean):
                Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<ResolutionCandidate>>? {
            if (isRecursiveVariableResolution(variable)) return null

            assert(variable.isSuccessful) {
                "Variable call should be successful: $variable " +
                        "Descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val variableCallArgument = createReceiverCallArgument(variable)

            val explicitReceiver = kotlinCall.explicitReceiver
            val callForInvoke = if (useExplicitReceiver && explicitReceiver != null) {
                PSIKotlinCallForInvoke(kotlinCall, variable, explicitReceiver, variableCallArgument)
            } else {
                PSIKotlinCallForInvoke(kotlinCall, variable, variableCallArgument, null)
            }

            return variableCallArgument.receiver to SimpleCandidateFactory(
                callComponents, scopeTower, callForInvoke, createResolutionCallbacks(context)
            )
        }

        // todo: create special check that there is no invoke on variable
        private fun isRecursiveVariableResolution(variable: ResolutionCandidate): Boolean {
            val variableType = variable.resolvedCall.candidateDescriptor.returnType
            return variableType is DeferredType && variableType.isComputing
        }

        // todo: review
        private fun createReceiverCallArgument(variable: ResolutionCandidate): SimpleKotlinCallArgument {
            variable.forceResolution()
            val variableReceiver = createReceiverValueWithSmartCastInfo(variable)
            if (variableReceiver.hasTypesFromSmartCasts()) {
                return ReceiverExpressionKotlinCallArgument(
                    createReceiverValueWithSmartCastInfo(variable),
                    isForImplicitInvoke = true
                )
            }

            val psiKotlinCall = variable.resolvedCall.atom.psiKotlinCall

            val variableResult = PartialCallResolutionResult(variable.resolvedCall, listOf(), variable.getSystem())

            return SubKotlinCallArgumentImpl(
                CallMaker.makeExternalValueArgument((variableReceiver.receiverValue as ExpressionReceiver).expression),
                psiKotlinCall.resultDataFlowInfo, psiKotlinCall.resultDataFlowInfo, variableReceiver,
                variableResult
            )
        }

        // todo: decrease hacks count
        private fun createReceiverValueWithSmartCastInfo(variable: ResolutionCandidate): ReceiverValueWithSmartCastInfo {
            val callForVariable = variable.resolvedCall.atom as PSIKotlinCallForVariable
            val calleeExpression = callForVariable.baseCall.psiCall.calleeExpression as? KtReferenceExpression
                ?: error("Unexpected call : ${callForVariable.baseCall.psiCall}")

            val temporaryTrace = TemporaryBindingTrace.create(context.trace, "Context for resolve candidate")

            val type = variable.resolvedCall.freshReturnType!!
            val variableReceiver = ExpressionReceiver.create(calleeExpression, type, temporaryTrace.bindingContext)

            temporaryTrace.record(BindingContext.REFERENCE_TARGET, calleeExpression, variable.resolvedCall.candidateDescriptor)
            val dataFlowValue =
                dataFlowValueFactory.createDataFlowValue(variableReceiver, temporaryTrace.bindingContext, context.scope.ownerDescriptor)
            return ReceiverValueWithSmartCastInfo(
                variableReceiver,
                context.dataFlowInfo.getCollectedTypes(dataFlowValue, context.languageVersionSettings).compactIfPossible(),
                dataFlowValue.isStable
            ).prepareReceiverRegardingCaptureTypes()
        }
    }

    private fun NewResolutionOldInference.ResolutionKind.toKotlinCallKind(): KotlinCallKind =
        when (this) {
            is NewResolutionOldInference.ResolutionKind.Function -> KotlinCallKind.FUNCTION
            is NewResolutionOldInference.ResolutionKind.Variable -> KotlinCallKind.VARIABLE
            is NewResolutionOldInference.ResolutionKind.Invoke -> KotlinCallKind.INVOKE
            is NewResolutionOldInference.ResolutionKind.CallableReference -> KotlinCallKind.CALLABLE_REFERENCE
            is NewResolutionOldInference.ResolutionKind.GivenCandidates -> KotlinCallKind.UNSUPPORTED
        }

    private fun toKotlinCall(
        context: BasicCallResolutionContext,
        kotlinCallKind: KotlinCallKind,
        oldCall: Call,
        name: Name,
        tracingStrategy: TracingStrategy,
        isSpecialFunction: Boolean,
        forcedExplicitReceiver: Receiver? = null,
    ): PSIKotlinCallImpl {
        val resolvedExplicitReceiver = resolveReceiver(
            context, forcedExplicitReceiver ?: oldCall.explicitReceiver, oldCall.isSafeCall(), isForImplicitInvoke = false
        )
        val dispatchReceiverForInvoke = resolveDispatchReceiverForInvoke(context, kotlinCallKind, oldCall)

        val resolvedTypeArguments = resolveTypeArguments(context, oldCall.typeArguments)

        val lambdasOutsideParenthesis = oldCall.functionLiteralArguments.size
        val extraArgumentsNumber = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) 1 else lambdasOutsideParenthesis

        val allValueArguments = oldCall.valueArguments
        val argumentsInParenthesis = if (extraArgumentsNumber == 0) allValueArguments else allValueArguments.dropLast(extraArgumentsNumber)

        val externalLambdaArguments = oldCall.functionLiteralArguments
        val resolvedArgumentsInParenthesis =
            resolveArgumentsInParenthesis(context, argumentsInParenthesis, isSpecialFunction, tracingStrategy)

        val externalArgument = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) {
            assert(externalLambdaArguments.isEmpty()) {
                "Unexpected lambda parameters for call $oldCall"
            }
            if (allValueArguments.isEmpty()) {
                throw KotlinExceptionWithAttachments("Can not find an external argument for 'set' method")
                    .withPsiAttachment("callElement.kt", oldCall.callElement)
                    .withPsiAttachment("file.kt", oldCall.callElement.takeIf { it.isValid }?.containingFile)
            }
            allValueArguments.last()
        } else {
            if (externalLambdaArguments.size > 1) {
                for (i in externalLambdaArguments.indices) {
                    if (i == 0) continue
                    val lambdaExpression = externalLambdaArguments[i].getLambdaExpression() ?: continue

                    if (lambdaExpression.isTrailingLambdaOnNewLine) {
                        context.trace.report(Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(lambdaExpression))
                    }
                    context.trace.report(Errors.MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(lambdaExpression))
                }
            }

            externalLambdaArguments.firstOrNull()
        }

        val dataFlowInfoAfterArgumentsInParenthesis =
            if (externalArgument != null && resolvedArgumentsInParenthesis.isNotEmpty())
                resolvedArgumentsInParenthesis.last().psiCallArgument.dataFlowInfoAfterThisArgument
            else
                context.dataFlowInfoForArguments.resultInfo

        val resolvedExternalArgument = externalArgument?.let {
            resolveValueArgument(context, dataFlowInfoAfterArgumentsInParenthesis, it, isSpecialFunction, tracingStrategy)
        }
        val resultDataFlowInfo = resolvedExternalArgument?.dataFlowInfoAfterThisArgument ?: dataFlowInfoAfterArgumentsInParenthesis

        resolvedArgumentsInParenthesis.forEach { it.setResultDataFlowInfoIfRelevant(resultDataFlowInfo) }
        resolvedExternalArgument?.setResultDataFlowInfoIfRelevant(resultDataFlowInfo)

        val isForImplicitInvoke = oldCall is CallTransformer.CallForImplicitInvoke

        return PSIKotlinCallImpl(
            kotlinCallKind, oldCall, tracingStrategy, resolvedExplicitReceiver, dispatchReceiverForInvoke, name,
            resolvedTypeArguments, resolvedArgumentsInParenthesis, resolvedExternalArgument, context.dataFlowInfo, resultDataFlowInfo,
            context.dataFlowInfoForArguments, isForImplicitInvoke
        )
    }

    private fun resolveDispatchReceiverForInvoke(
        context: BasicCallResolutionContext,
        kotlinCallKind: KotlinCallKind,
        oldCall: Call
    ): ReceiverKotlinCallArgument? {
        if (kotlinCallKind != KotlinCallKind.INVOKE) return null

        require(oldCall is CallTransformer.CallForImplicitInvoke) { "Call should be CallForImplicitInvoke, but it is: $oldCall" }

        return resolveReceiver(context, oldCall.dispatchReceiver, isSafeCall = false, isForImplicitInvoke = true)
    }

    private fun resolveReceiver(
        context: BasicCallResolutionContext,
        oldReceiver: Receiver?,
        isSafeCall: Boolean,
        isForImplicitInvoke: Boolean
    ): ReceiverKotlinCallArgument? {
        return when (oldReceiver) {
            null -> null

            is QualifierReceiver -> QualifierReceiverKotlinCallArgument(oldReceiver) // todo report warning if isSafeCall

            is ReceiverValue -> {
                if (oldReceiver is ExpressionReceiver) {
                    val ktExpression = KtPsiUtil.getLastElementDeparenthesized(oldReceiver.expression, context.statementFilter)

                    val bindingContext = context.trace.bindingContext
                    val call =
                        bindingContext[BindingContext.DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL, ktExpression]
                            ?: ktExpression?.getCall(bindingContext)

                    val partiallyResolvedCall = call?.let { bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)?.result }

                    if (partiallyResolvedCall != null) {
                        val receiver = ReceiverValueWithSmartCastInfo(oldReceiver, emptySet(), isStable = true)
                        return SubKotlinCallArgumentImpl(
                            CallMaker.makeExternalValueArgument(oldReceiver.expression),
                            context.dataFlowInfo, context.dataFlowInfo, receiver, partiallyResolvedCall
                        )
                    }
                }

                ReceiverExpressionKotlinCallArgument(
                    context.transformToReceiverWithSmartCastInfo(oldReceiver),
                    isSafeCall,
                    isForImplicitInvoke
                )
            }

            else -> error("Incorrect receiver: $oldReceiver")
        }
    }

    private fun resolveTypeArguments(context: BasicCallResolutionContext, typeArguments: List<KtTypeProjection>): List<TypeArgument> =
        typeArguments.map { projection ->
            if (projection.projectionKind != KtProjectionKind.NONE) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
            }
            ModifierCheckerCore.check(projection, context.trace, null, languageVersionSettings)

            val typeReference = projection.typeReference ?: return@map TypeArgumentPlaceholder

            if (typeReference.isPlaceholder) {
                val resolvedAnnotations = typeResolver.resolveTypeAnnotations(context.trace, context.scope, typeReference)
                    .apply(ForceResolveUtil::forceResolveAllContents)

                for (annotation in resolvedAnnotations) {
                    val annotationElement = annotation.source.getPsi() ?: continue
                    context.trace.report(Errors.UNSUPPORTED.on(annotationElement, "annotations on an underscored type argument"))
                }

                if (!arePartiallySpecifiedTypeArgumentsEnabled) {
                    context.trace.report(Errors.UNSUPPORTED.on(typeReference, "underscored type argument"))
                }

                return@map TypeArgumentPlaceholder
            }

            SimpleTypeArgumentImpl(projection, resolveType(context, typeReference, typeResolver))
        }

    private fun resolveArgumentsInParenthesis(
        context: BasicCallResolutionContext,
        arguments: List<ValueArgument>,
        isSpecialFunction: Boolean,
        tracingStrategy: TracingStrategy,
    ): List<KotlinCallArgument> {
        val dataFlowInfoForArguments = context.dataFlowInfoForArguments
        return arguments.map { argument ->
            resolveValueArgument(
                context,
                dataFlowInfoForArguments.getInfo(argument),
                argument,
                isSpecialFunction,
                tracingStrategy
            ).also { resolvedArgument ->
                dataFlowInfoForArguments.updateInfo(argument, resolvedArgument.dataFlowInfoAfterThisArgument)
            }
        }
    }

    private fun resolveValueArgument(
        outerCallContext: BasicCallResolutionContext,
        startDataFlowInfo: DataFlowInfo,
        valueArgument: ValueArgument,
        isSpecialFunction: Boolean,
        tracingStrategy: TracingStrategy,
    ): PSIKotlinCallArgument {
        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns

        fun createParseErrorElement() = ParseErrorKotlinCallArgument(valueArgument, startDataFlowInfo)

        val argumentExpression = valueArgument.getArgumentExpression() ?: return createParseErrorElement()
        val ktExpression = KtPsiUtil.deparenthesize(argumentExpression) ?: createParseErrorElement()

        val argumentName = valueArgument.getArgumentName()?.asName

        @Suppress("NAME_SHADOWING")
        val outerCallContext = outerCallContext.expandContextForCatchClause(ktExpression)

        processFunctionalExpression(
            outerCallContext, argumentExpression, startDataFlowInfo,
            valueArgument, argumentName, builtIns, typeResolver
        )?.let {
            return it
        }

        if (ktExpression is KtCollectionLiteralExpression) {
            return CollectionLiteralKotlinCallArgumentImpl(
                valueArgument, argumentName, startDataFlowInfo, startDataFlowInfo, ktExpression, outerCallContext
            )
        }

        val context = outerCallContext.replaceContextDependency(ContextDependency.DEPENDENT)
            .replaceDataFlowInfo(startDataFlowInfo)
            .let {
                if (isSpecialFunction &&
                    argumentExpression is KtBlockExpression &&
                    ArgumentTypeResolver.getCallableReferenceExpressionIfAny(argumentExpression, it) != null
                ) {
                    it
                } else {
                    it.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
                }
            }

        if (ktExpression is KtCallableReferenceExpression) {
            return createCallableReferenceKotlinCallArgument(
                context, ktExpression, startDataFlowInfo, valueArgument, argumentName, outerCallContext, tracingStrategy
            )
        }

        // argumentExpression instead of ktExpression is hack -- type info should be stored also for parenthesized expression
        val typeInfo = expressionTypingServices.getTypeInfo(argumentExpression, context)

        return createSimplePSICallArgument(context, valueArgument, typeInfo) ?: createParseErrorElement()
    }

    fun getLhsResult(context: BasicCallResolutionContext, ktExpression: KtCallableReferenceExpression): Pair<DoubleColonLHS?, LHSResult> {
        val expressionTypingContext = ExpressionTypingContext.newContext(context)

        if (ktExpression.isEmptyLHS) return null to LHSResult.Empty

        val doubleColonLhs = (context.callPosition as? CallPosition.CallableReferenceRhs)?.lhs
            ?: doubleColonExpressionResolver.resolveDoubleColonLHS(ktExpression, expressionTypingContext)
            ?: return null to LHSResult.Empty
        val lhsResult = when (doubleColonLhs) {
            is DoubleColonLHS.Expression -> {
                if (doubleColonLhs.isObjectQualifier) {
                    val classifier = doubleColonLhs.type.constructor.declarationDescriptor
                    val calleeExpression = ktExpression.receiverExpression?.getCalleeExpressionIfAny()
                    if (calleeExpression is KtSimpleNameExpression && classifier is ClassDescriptor) {
                        LHSResult.Object(ClassQualifier(calleeExpression, classifier))
                    } else {
                        LHSResult.Error
                    }
                } else {
                    val fakeArgument = FakeValueArgumentForLeftCallableReference(ktExpression)

                    val kotlinCallArgument = createSimplePSICallArgument(context, fakeArgument, doubleColonLhs.typeInfo)
                    kotlinCallArgument?.let { LHSResult.Expression(it as SimpleKotlinCallArgument) } ?: LHSResult.Error
                }
            }
            is DoubleColonLHS.Type -> {
                val qualifiedExpression = ktExpression.receiverExpression!!
                val qualifier = expressionTypingContext.trace.get(BindingContext.QUALIFIER, qualifiedExpression)
                val classifier = doubleColonLhs.type.constructor.declarationDescriptor
                if (classifier !is ClassDescriptor) {
                    expressionTypingContext.trace.report(Errors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(ktExpression))
                    LHSResult.Error
                } else {
                    LHSResult.Type(qualifier, doubleColonLhs.type.unwrap())
                }
            }
        }

        return doubleColonLhs to lhsResult
    }

    fun createCallableReferenceKotlinCallArgument(
        context: BasicCallResolutionContext,
        ktExpression: KtCallableReferenceExpression,
        startDataFlowInfo: DataFlowInfo,
        valueArgument: ValueArgument,
        argumentName: Name?,
        outerCallContext: BasicCallResolutionContext,
        tracingStrategy: TracingStrategy
    ): CallableReferenceKotlinCallArgumentImpl {
        checkNoSpread(outerCallContext, valueArgument)

        val (doubleColonLhs, lhsResult) = getLhsResult(context, ktExpression)
        val newDataFlowInfo = (doubleColonLhs as? DoubleColonLHS.Expression)?.dataFlowInfo ?: startDataFlowInfo
        val rhsExpression = ktExpression.callableReference
        val rhsName = rhsExpression.getReferencedNameAsName()
        val call = outerCallContext.trace[BindingContext.CALL, rhsExpression]
            ?: CallMaker.makeCall(rhsExpression, null, null, rhsExpression, emptyList())
        val kotlinCall = toKotlinCall(context, KotlinCallKind.CALLABLE_REFERENCE, call, rhsName, tracingStrategy, isSpecialFunction = false)

        return CallableReferenceKotlinCallArgumentImpl(
            ASTScopeTower(context, rhsExpression), valueArgument, startDataFlowInfo,
            newDataFlowInfo, ktExpression, argumentName, lhsResult, rhsName, kotlinCall
        )
    }


    private fun BasicCallResolutionContext.expandContextForCatchClause(ktExpression: Any): BasicCallResolutionContext {
        if (ktExpression !is KtExpression) return this

        val variableDescriptorHolder = trace.bindingContext[NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER, ktExpression] ?: return this
        val variableDescriptor = variableDescriptorHolder.get() ?: return this
        variableDescriptorHolder.set(null)

        val redeclarationChecker = expressionTypingServices.createLocalRedeclarationChecker(trace)

        val catchScope = with(scope) {
            LexicalWritableScope(this, ownerDescriptor, false, redeclarationChecker, LexicalScopeKind.CATCH)
        }
        val isReferencingToUnderscoreNamedParameterForbidden =
            languageVersionSettings.getFeatureSupport(LanguageFeature.ForbidReferencingToUnderscoreNamedParameterOfCatchBlock) == LanguageFeature.State.ENABLED
        if (!variableDescriptor.isUnderscoreNamed || !isReferencingToUnderscoreNamedParameterForbidden) {
            catchScope.addVariableDescriptor(variableDescriptor)
        }
        return replaceScope(catchScope)
    }
}

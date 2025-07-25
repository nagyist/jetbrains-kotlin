/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isBuiltin
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.constant.AnnotationValue
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.constant.EnumValue
import org.jetbrains.kotlin.constant.IntValue
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.comparators.FirCallableDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.deserialization.projection
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.canBeUsedForConstVal
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.typeAttributeExtensions
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirScriptDeclarationsScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.serialization.constant.hasConstantValue
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.metadata.deserialization.isKotlin1Dot4OrLater
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.metadata.serialization.MutableTypeTable
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.protobuf.ByteString
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.resolve.RequireKotlinConstants
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToIndex

class FirElementSerializer private constructor(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
    private val currentDeclaration: FirDeclaration?,
    private val typeParameters: Interner<FirTypeParameter>,
    private val extension: FirSerializerExtension,
    val typeTable: MutableTypeTable,
    private val versionRequirementTable: MutableVersionRequirementTable?,
    private val serializeTypeTableToFunction: Boolean,
    private val typeApproximator: AbstractTypeApproximator,
    private val languageVersionSettings: LanguageVersionSettings,
    private val produceHeaderKlib: Boolean,
) : SessionAndScopeSessionHolder {
    private val contractSerializer = FirContractSerializer()
    private val providedDeclarationsService = session.providedDeclarationsForMetadataService
    private val stdLibCompilation = languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)

    private var metDefinitelyNotNullType: Boolean = false

    fun packagePartProto(file: FirFile, actualizedExpectDeclarations: Set<FirDeclaration>?): ProtoBuf.Package.Builder {
        val builder = ProtoBuf.Package.newBuilder()

        extension.processFile(file) {
            for (declaration in file.declarations) {
                builder.addDeclarationProto(declaration, actualizedExpectDeclarations) {}
            }
        }

        return finalizePackagePartProto(file.packageFqName, builder, actualizedExpectDeclarations)
    }

    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    annotation class SensitiveApi

    /**
     * This method doesn't use `extension.constValueProvider`, so if there is a provider, then
     *   constants might be computed incorrectly
     */
    @SensitiveApi
    fun packagePartProto(
        packageFqName: FqName,
        declarations: List<FirDeclaration>,
        actualizedExpectDeclarations: Set<FirDeclaration>?
    ): ProtoBuf.Package.Builder {
        require(extension.constValueProvider == null) {
            "constValueProvider cannot work without file. Please use the `packagePartProto` overload which accepts FirFile"
        }
        val builder = ProtoBuf.Package.newBuilder()
        for (declaration in declarations) {
            builder.addDeclarationProto(declaration, actualizedExpectDeclarations) {}
        }
        return finalizePackagePartProto(packageFqName, builder, actualizedExpectDeclarations)
    }

    private fun ProtoBuf.Package.Builder.addDeclarationProto(
        declaration: FirDeclaration,
        actualizedExpectDeclarations: Set<FirDeclaration>?,
        onUnsupportedDeclaration: (FirDeclaration) -> Unit,
    ) {
        if (declaration is FirMemberDeclaration) {
            if (!declaration.isNotExpectOrShouldBeSerialized(actualizedExpectDeclarations)) return
            if (!declaration.isNotPrivateOrShouldBeSerialized(produceHeaderKlib)) return
            when (declaration) {
                is FirProperty -> propertyProto(declaration)?.let { this.addProperty(it) }
                is FirSimpleFunction -> functionProto(declaration)?.let { this.addFunction(it) }
                is FirTypeAlias -> typeAliasProto(declaration)?.let { this.addTypeAlias(it) }
                else -> onUnsupportedDeclaration(declaration)
            }
        } else {
            onUnsupportedDeclaration(declaration)
        }
    }

    private fun finalizePackagePartProto(
        packageFqName: FqName,
        builder: ProtoBuf.Package.Builder,
        actualizedExpectDeclarations: Set<FirDeclaration>?,
    ): ProtoBuf.Package.Builder {
        extension.serializePackage(packageFqName, builder, versionRequirementTable, this)
        // Next block will process declarations from plugins.
        // Such declarations don't belong to any file, so there is no need to call `extension.processFile`.
        for (declaration in providedDeclarationsService.getProvidedTopLevelDeclarations(packageFqName, scopeSession)) {
            builder.addDeclarationProto(declaration, actualizedExpectDeclarations) {
                error("Unsupported top-level declaration type: ${it.render()}")
            }
        }

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable?.serialize()?.let { builder.versionRequirementTable = it }

        return builder
    }

    fun classProto(klass: FirClass, containingFile: FirFile?): ProtoBuf.Class.Builder {
        return if (containingFile == null) {
            // Containing file can be null for local classes, as well as synthetic ones like kotlin/Cloneable.
            // Not using `processFile` means that we will not be able to use IR-based constant expression evaluator when serializing
            // annotations in such classes, and will fall back to the FIR-based evaluator.
            classProtoImpl(klass)
        } else extension.processFile(containingFile) {
            classProtoImpl(klass)
        }
    }

    private fun classProtoImpl(klass: FirClass): ProtoBuf.Class.Builder = whileAnalysing(session, klass) {
        val builder = ProtoBuf.Class.newBuilder()

        val regularClass = klass as? FirRegularClass
        val modality = regularClass?.modality ?: Modality.FINAL

        val hasEnumEntries = klass.classKind == ClassKind.ENUM_CLASS && languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)
        val flags = Flags.getClassFlags(
            klass.nonSourceAnnotations(session).isNotEmpty() || extension.hasAdditionalAnnotations(klass),
            ProtoEnumFlags.visibility(regularClass?.let { normalizeVisibility(it) } ?: Visibilities.Local),
            ProtoEnumFlags.modality(modality),
            ProtoEnumFlags.classKind(klass.classKind, regularClass?.isCompanion == true),
            regularClass?.isInner == true,
            regularClass?.isData == true,
            regularClass?.isExternal == true,
            regularClass?.isExpect == true,
            regularClass?.isInlineOrValue == true,
            regularClass?.isFun == true,
            hasEnumEntries,
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.fqName = getClassifierId(klass)

        for (typeParameter in klass.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            builder.addTypeParameter(typeParameterProto(typeParameter))
        }

        val classSymbol = klass.symbol
        val classId = classSymbol.classId
        if (classId != StandardClassIds.Any && classId != StandardClassIds.Nothing) {
            // Special classes (Any, Nothing) have no supertypes
            for (superTypeRef in extension.getClassSupertypes(klass)) {
                if (useTypeTable()) {
                    builder.addSupertypeId(typeId(superTypeRef))
                } else {
                    builder.addSupertype(typeProto(superTypeRef))
                }
            }
        }


        /*
         * Order of constructors:
         *   - declared constructors in declaration order
         *   - generated constructors in sorted order
         *   - provided constructors in sorted order
         */
        if (regularClass != null && regularClass.classKind != ClassKind.ENUM_ENTRY) {
            for (constructor in regularClass.constructors()) {
                if (!constructor.isNotPrivateOrShouldBeSerialized(produceHeaderKlib)) continue
                builder.addConstructor(constructorProto(constructor))
            }

            val providedConstructors = providedDeclarationsService
                .getProvidedConstructors(classSymbol, scopeSession)
                .sortedWith(FirCallableDeclarationComparator)
            for (constructor in providedConstructors) {
                builder.addConstructor(constructorProto(constructor))
            }
        }

        val providedCallables = providedDeclarationsService
            .getProvidedCallables(classSymbol, scopeSession)
            .sortedWith(FirCallableDeclarationComparator)

        /*
         * Order of callables:
         *   - declared callable in declaration order
         *   - generated callable in sorted order
         *   - provided callable in sorted order
         */
        val callableMembers = (klass.memberDeclarations() + providedCallables)

        for (declaration in callableMembers) {
            if (declaration !is FirEnumEntry && declaration.isStatic) continue // ??? Miss values() & valueOf()
            if (!declaration.isNotPrivateOrShouldBeSerialized(produceHeaderKlib)) continue
            // We have such declarations when compiling stdlib, but we don't need them as serialized bultins metadata
            if (declaration.origin == FirDeclarationOrigin.Enhancement
                || declaration.origin == FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk) {
                continue
            }
            when (declaration) {
                is FirProperty -> propertyProto(declaration)?.let { builder.addProperty(it) }
                is FirSimpleFunction -> functionProto(declaration)?.let { builder.addFunction(it) }
                is FirEnumEntry -> enumEntryProto(declaration).let { builder.addEnumEntry(it) }
                else -> {}
            }
        }

        val nestedClassifiers = computeNestedClassifiersForClass(classSymbol)
        for (nestedClassifier in nestedClassifiers) {
            if (nestedClassifier is FirTypeAliasSymbol) {
                typeAliasProto(nestedClassifier.fir)?.let { builder.addTypeAlias(it) }
            } else if (nestedClassifier is FirRegularClassSymbol) {
                builder.addNestedClassName(getSimpleNameIndex(nestedClassifier.name))
            }
        }

        if (klass is FirRegularClass && klass.modality == Modality.SEALED) {
            val inheritors = klass.getSealedClassInheritors(session)
            for (inheritorId in inheritors) {
                builder.addSealedSubclassFqName(stringTable.getQualifiedClassNameIndex(inheritorId))
            }
        }

        val companionObject = regularClass?.companionObjectSymbol?.fir
        if (companionObject != null) {
            builder.companionObjectName = getSimpleNameIndex(companionObject.name)
        }

        when (val representation = (klass as? FirRegularClass)?.valueClassRepresentation) {
            is InlineClassRepresentation -> {
                builder.inlineClassUnderlyingPropertyName = getSimpleNameIndex(representation.underlyingPropertyName)

                val property = callableMembers.single {
                    it is FirProperty && it.receiverParameter == null && it.name == representation.underlyingPropertyName
                }

                if (!property.visibility.isPublicAPI) {
                    if (useTypeTable()) {
                        builder.inlineClassUnderlyingTypeId = typeId(representation.underlyingType)
                    } else {
                        builder.setInlineClassUnderlyingType(typeProto(representation.underlyingType))
                    }
                }
            }
            is MultiFieldValueClassRepresentation -> {}
            null -> {}
        }

        if (klass is FirRegularClass) {
            for (contextParameter in klass.contextParameters) {
                val typeRef = contextParameter.returnTypeRef
                if (useTypeTable()) {
                    builder.addContextReceiverTypeId(typeId(typeRef))
                } else {
                    builder.addContextReceiverType(typeProto(contextParameter.returnTypeRef))
                }
            }
        }

        if (versionRequirementTable == null) error("Version requirements must be serialized for classes: ${klass.render()}")

        builder.addAllVersionRequirement(versionRequirementTable.serializeVersionRequirements(klass))

        extension.serializeClass(klass, builder, versionRequirementTable, this)

        if (metDefinitelyNotNullType) {
            builder.addVersionRequirement(
                writeLanguageVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes, versionRequirementTable)
            )
        }

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable.serialize()?.let { builder.versionRequirementTable = it }

        if (klass is FirRegularClass) {
            @OptIn(FirExtensionApiInternals::class)
            for (plugin in session.extensionService.metadataSerializerPlugins) {
                val protoRegistrar = object : FirMetadataSerializerPlugin.ProtoRegistrar {
                    override fun <Type> setExtension(
                        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Class, Type>,
                        value: Type,
                    ) {
                        builder.setExtension(extension, value)
                    }
                }
                plugin.registerProtoExtensions(klass.symbol, stringTable, protoRegistrar)
            }
        }
        builder.serializeCompilerPluginMetadata(klass, ProtoBuf.Class.Builder::addCompilerPluginData)
        return builder
    }

    @OptIn(UnexpandedTypeCheck::class)
    fun scriptProto(script: FirScript): ProtoBuf.Class.Builder = whileAnalysing(session, script) {
        val builder = ProtoBuf.Class.newBuilder()

        val flags = Flags.getClassFlags(
            extension.hasAdditionalAnnotations(script),
            ProtoEnumFlags.visibility(Visibilities.Public),
            ProtoEnumFlags.modality(Modality.FINAL),
            ProtoEnumFlags.classKind(ClassKind.CLASS, false),
            /* inner = */ false,
            /* isData = */ false,
            /* isExternal = */ false,
            /* isExpect = */ false,
            /* isValue = */ false,
            /* isFun = */ false,
            /* hasEnumEntries = */ false,
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        val classId = scriptClassId(script)

        builder.fqName = getClassifierId(classId)

        val memberScope = FirScriptDeclarationsScope(session, script)

        val callableMembers = buildList {
            memberScope.processAllCallables { add(it.fir) }
        }

        for (declaration in callableMembers) {
            when (declaration) {
                is FirProperty -> {
                    val skipPropertyMetadata = when {
                        declaration.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> {
                            declaration.returnTypeRef.let { (it.isUnit || it.isNothing || it.isNullableNothing) }
                        }
                        declaration.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR -> { // '_' DD element
                            declaration.destructuringDeclarationContainerVariable != null
                        }
                        else -> false
                    }
                    if (!skipPropertyMetadata) {
                        propertyProto(declaration)?.let { builder.addProperty(it) }
                    }
                }
                is FirSimpleFunction -> functionProto(declaration)?.let { builder.addFunction(it) }
                else -> {}
            }
        }

        memberScope.getClassifierNames().forEach { classifierName ->
            memberScope.processClassifiersByName(classifierName) { nestedClassifier ->
                when (nestedClassifier) {
                    is FirRegularClassSymbol -> {
                        builder.addNestedClassName(getSimpleNameIndex(nestedClassifier.name))
                    }
                    else -> {}
                }
            }
        }

        if (versionRequirementTable == null) error("Version requirements must be serialized for scripts: ${script.render()}")

        builder.addAllVersionRequirement(versionRequirementTable.serializeVersionRequirements(script))

        extension.serializeScript(script, builder, versionRequirementTable, this)

        if (metDefinitelyNotNullType) {
            builder.addVersionRequirement(
                writeLanguageVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes, versionRequirementTable)
            )
        }

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable.serialize()?.let { builder.versionRequirementTable = it }

        return builder
    }

    @OptIn(UnexpandedTypeCheck::class)
    fun snippetProto(snippet: FirReplSnippet): ProtoBuf.Class.Builder = whileAnalysing(session, snippet) {
        val builder = ProtoBuf.Class.newBuilder()

        val flags = Flags.getClassFlags(
            extension.hasAdditionalAnnotations(snippet),
            ProtoEnumFlags.visibility(Visibilities.Public),
            ProtoEnumFlags.modality(Modality.FINAL),
            ProtoEnumFlags.classKind(ClassKind.CLASS, false),
            /* inner = */ false,
            /* isData = */ false,
            /* isExternal = */ false,
            /* isExpect = */ false,
            /* isValue = */ false,
            /* isFun = */ false,
            /* hasEnumEntries = */ false,
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        val classId = snippetClassId(snippet)

        builder.fqName = getClassifierId(classId)

        for (statement in snippet.body.statements) {
            val declaration = statement as? FirDeclaration ?: continue
            when (declaration) {
                is FirProperty -> propertyProto(declaration)?.let { builder.addProperty(it) }
                is FirSimpleFunction -> functionProto(declaration)?.let { builder.addFunction(it) }
                is FirRegularClass -> builder.addNestedClassName(getSimpleNameIndex(declaration.name))
                is FirTypeAlias -> typeAliasProto(declaration)?.let { builder.addTypeAlias(it) }
                else -> {}
            }
        }

        if (versionRequirementTable == null) error("Version requirements must be serialized for snippets: ${snippet.render()}")

        builder.addAllVersionRequirement(versionRequirementTable.serializeVersionRequirements(snippet))

        extension.serializeSnippet(snippet, builder, versionRequirementTable, this)

        if (metDefinitelyNotNullType) {
            builder.addVersionRequirement(
                writeLanguageVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes, versionRequirementTable)
            )
        }

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable.serialize()?.let { builder.versionRequirementTable = it }

        return builder
    }

    /*
     * Order of nested classifiers:
     *   - declared classifiers in declaration order
     *   - generated classifiers in sorted order
     */
    fun computeNestedClassifiersForClass(classSymbol: FirClassSymbol<*>): List<FirClassifierSymbol<*>> {
        val scope = session.nestedClassifierScope(classSymbol.fir) ?: return emptyList()
        return buildList {
            val indexByDeclaration = classSymbol.fir.declarations.filterIsInstance<FirClassLikeDeclaration>().mapToIndex()
            val (declared, nonDeclared) = scope.getClassifierNames()
                .mapNotNull { scope.getSingleClassifier(it)?.fir as FirClassLikeDeclaration? }
                .partition { it in indexByDeclaration }
            declared.sortedBy { indexByDeclaration.getValue(it) }.mapTo(this) { it.symbol }
            nonDeclared.sortedWith(FirMemberDeclarationComparator).mapTo(this) { it.symbol }
        }
    }

    private fun FirClass.memberDeclarations(): List<FirCallableDeclaration> {
        return collectDeclarations<FirCallableDeclaration, FirCallableSymbol<*>> { memberScope, addDeclarationIfNeeded ->
            memberScope.processAllFunctions { addDeclarationIfNeeded(it) }
            memberScope.processAllProperties { addDeclarationIfNeeded(it) }
        }
    }

    private fun FirClass.constructors(): List<FirConstructor> {
        return collectDeclarations { memberScope, addDeclarationIfNeeded ->
            memberScope.processDeclaredConstructors { addDeclarationIfNeeded(it) }
        }
    }

    private inline fun <reified T : FirCallableDeclaration, S : FirCallableSymbol<*>> FirClass.collectDeclarations(
        processScope: (FirTypeScope, ((S) -> Unit)) -> Unit
    ): List<T> {
        val foundInScope = buildList {
            val memberScope = unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
            processScope(memberScope) {
                val declaration = it.fir as T
                val dispatchReceiverLookupTag = declaration.dispatchReceiverClassLookupTagOrNull()
                // Special case for data/value class equals/hashCode/toString, see KT-57510
                val isFakeOverrideOfAnyFunctionInDataOrValueClass = this@collectDeclarations is FirRegularClass &&
                        (this@collectDeclarations.isData || this@collectDeclarations.isInlineOrValue) &&
                        dispatchReceiverLookupTag?.classId == StandardClassIds.Any && !declaration.isFinal
                // Related: https://youtrack.jetbrains.com/issue/KT-20427#focus=Comments-27-8652759.0-0
                if (isFakeOverrideOfAnyFunctionInDataOrValueClass && !this@collectDeclarations.isExpect ||
                    !declaration.isSubstitutionOrIntersectionOverride &&
                    (declaration.isStatic || declaration is FirConstructor || dispatchReceiverLookupTag == this@collectDeclarations.symbol.toLookupTag())
                ) {
                    add(declaration)
                }
            }

            for (declaration in declarations) {
                if (declaration is T && declaration.isStatic) {
                    add(declaration)
                }
            }
        }
        val indexByDeclaration = declarations.filterIsInstance<T>().mapToIndex()
        val (declared, nonDeclared) = foundInScope
            .sortedBy { indexByDeclaration[it] ?: Int.MAX_VALUE }
            .partition { it in indexByDeclaration }
        return declared + nonDeclared.sortedWith(FirCallableDeclarationComparator)
    }

    private fun FirPropertyAccessor.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
        (this as FirAnnotationContainer).nonSourceAnnotations(session)

    fun propertyProto(property: FirProperty): ProtoBuf.Property.Builder? = whileAnalysing(session, property) {
        val builder = ProtoBuf.Property.newBuilder()

        val local = createChildSerializer(property)

        var hasGetter = false
        var hasSetter = false

        val hasAnnotations = property.nonSourceAnnotations(session).isNotEmpty()
                || property.backingField?.nonSourceAnnotations(session)?.isNotEmpty() == true
                || extension.hasAdditionalAnnotations(property)
                || property.backingField?.let { extension.hasAdditionalAnnotations(it) } == true

        val modality = property.modality!!
        val defaultAccessorFlags = Flags.getAccessorFlags(
            hasAnnotations,
            ProtoEnumFlags.visibility(normalizeVisibility(property)),
            ProtoEnumFlags.modality(modality),
            false, false, false
        )

        val getter = property.getter ?: with(property) {
            if (origin == FirDeclarationOrigin.Delegated) {
                // since we generate the default accessor on fir2ir anyway (Fir2IrDeclarationStorage.createIrProperty), we have to
                // serialize it accordingly at least for delegates to fix issues like #KT-57373
                // TODO: rewrite accordingly after fixing #KT-58233
                FirDefaultPropertyGetter(
                    source = null,
                    moduleData = moduleData,
                    origin = origin,
                    propertyTypeRef = returnTypeRef,
                    visibility = visibility,
                    propertySymbol = symbol,
                    modality = modality,
                )
            } else null
        }
        if (getter != null) {
            hasGetter = true
            val accessorFlags = getAccessorFlags(getter, property)
            if (accessorFlags != defaultAccessorFlags) {
                builder.getterFlags = accessorFlags
            }
        }

        val setter = property.setter ?: with(property) {
            if (origin == FirDeclarationOrigin.Delegated && isVar) {
                // since we generate the default accessor on fir2ir anyway (Fir2IrDeclarationStorage.createIrProperty), we have to
                // serialize it accordingly at least for delegates to fix issues like #KT-57373
                // TODO: rewrite accordingly after fixing #KT-58233
                FirDefaultPropertySetter(
                    source = null,
                    moduleData = moduleData,
                    origin = origin,
                    propertyTypeRef = returnTypeRef,
                    visibility = visibility,
                    propertySymbol = symbol,
                    modality = modality,
                )
            } else null
        }
        if (setter != null) {
            hasSetter = true
            val accessorFlags = getAccessorFlags(setter, property)
            if (accessorFlags != defaultAccessorFlags) {
                builder.setterFlags = accessorFlags
            }

            val nonSourceAnnotations = setter.nonSourceAnnotations(session)
            if (Flags.IS_NOT_DEFAULT.get(accessorFlags)) {
                val setterLocal = local.createChildSerializer(setter)
                for ((index, valueParameterDescriptor) in setter.valueParameters.withIndex()) {
                    val annotations = nonSourceAnnotations.filter { it.useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER }
                    builder.setSetterValueParameter(setterLocal.valueParameterProto(valueParameterDescriptor, index, setter, annotations))
                }
            }
        }

        val hasConstant = property.isConst || (!property.isVar
                && property.returnTypeRef.coneType.fullyExpandedType().canBeUsedForConstVal()
                && property.symbol.resolvedInitializer.hasConstantValue(session))
        val flags = Flags.getPropertyFlags(
            hasAnnotations,
            ProtoEnumFlags.visibility(normalizeVisibility(property)),
            ProtoEnumFlags.modality(modality),
            property.memberKind(),
            property.isVar, hasGetter, hasSetter, hasConstant, property.isConst, property.isLateInit,
            property.isExternal, property.delegateFieldSymbol != null, property.isExpect,
            property.status.hasMustUseReturnValue
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(property.name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(property.returnTypeRef, toSuper = true)
        } else {
            builder.setReturnType(local.typeProto(property.returnTypeRef, toSuper = true))
        }

        for (typeParameter in property.typeParameters) {
            builder.addTypeParameter(local.typeParameterProto(typeParameter))
        }

        for (contextParameter in property.contextParameters) {
            val typeRef = contextParameter.returnTypeRef
            if (useTypeTable()) {
                builder.addContextReceiverTypeId(local.typeId(typeRef))
            } else {
                builder.addContextReceiverType(local.typeProto(contextParameter.returnTypeRef))
            }
            builder.addContextParameter(
                local.valueParameterProto(
                    contextParameter,
                    additionalAnnotations = emptyList(),
                    declaresDefaultValue = false
                )
            )
        }

        val receiverParameter = property.receiverParameter
        if (receiverParameter != null) {
            val receiverTypeRef = receiverParameter.typeRef
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverTypeRef)
            } else {
                builder.setReceiverType(local.typeProto(receiverTypeRef))
            }
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(property))

            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes))
            }
        }

        extension.serializeProperty(property, builder, versionRequirementTable, local)
        builder.serializeCompilerPluginMetadata(property, ProtoBuf.Property.Builder::addCompilerPluginData)

        return builder
    }

    private fun FirProperty.memberKind(): MemberKind {
        return when (origin) {
            FirDeclarationOrigin.Delegated -> MemberKind.DELEGATION
            else -> MemberKind.DECLARATION
        }
    }

    fun functionProto(function: FirFunction): ProtoBuf.Function.Builder? = whileAnalysing(session, function) {
        val builder = ProtoBuf.Function.newBuilder()
        val simpleFunction = function as? FirSimpleFunction

        val local = createChildSerializer(function)

        val flags = Flags.getFunctionFlags(
            function.nonSourceAnnotations(session).isNotEmpty() || extension.hasAdditionalAnnotations(function),
            ProtoEnumFlags.visibility(simpleFunction?.let { normalizeVisibility(it) } ?: Visibilities.Local),
            ProtoEnumFlags.modality(simpleFunction?.modality ?: Modality.FINAL),
            function.memberKind(),
            simpleFunction?.isOperator == true,
            simpleFunction?.isInfix == true,
            simpleFunction?.isInline == true,
            simpleFunction?.isTailRec == true,
            simpleFunction?.isExternal == true,
            function.isSuspend,
            simpleFunction?.isExpect == true,
            shouldSetStableParameterNames(function),
            simpleFunction?.status?.hasMustUseReturnValue == true,
        )

        if (flags != builder.flags) {
            builder.flags = flags
        }

        val name = when (function) {
            is FirSimpleFunction -> {
                function.name
            }
            is FirAnonymousFunction -> {
                if (function.isLambda) SpecialNames.ANONYMOUS else SpecialNames.NO_NAME_PROVIDED
            }
            else -> throw AssertionError("Unsupported function: ${function.render()}")
        }
        builder.name = getSimpleNameIndex(name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(function.returnTypeRef, toSuper = true)
        } else {
            builder.setReturnType(local.typeProto(function.returnTypeRef, toSuper = true))
        }

        for (typeParameter in function.typeParameters) {
            builder.addTypeParameter(local.typeParameterProto(typeParameter))
        }

        for (contextParameter in function.contextParameters) {
            val typeRef = contextParameter.returnTypeRef
            if (useTypeTable()) {
                builder.addContextReceiverTypeId(local.typeId(typeRef))
            } else {
                builder.addContextReceiverType(local.typeProto(contextParameter.returnTypeRef))
            }
            builder.addContextParameter(
                local.valueParameterProto(
                    contextParameter,
                    additionalAnnotations = emptyList(),
                    declaresDefaultValue = false
                )
            )
        }

        val receiverParameter = function.receiverParameter
        if (receiverParameter != null) {
            val receiverTypeRef = receiverParameter.typeRef
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverTypeRef)
            } else {
                builder.setReceiverType(local.typeProto(receiverTypeRef))
            }
        }

        for ((index, valueParameter) in function.valueParameters.withIndex()) {
            builder.addValueParameter(local.valueParameterProto(valueParameter, index, function))
        }

        contractSerializer.serializeContractOfFunctionIfAny(function, builder, local)

        extension.serializeFunction(function, builder, versionRequirementTable, local)

        if (serializeTypeTableToFunction) {
            typeTable.serialize()?.let { builder.typeTable = it }
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(function))

            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes))
            }
        }

        builder.serializeCompilerPluginMetadata(function, ProtoBuf.Function.Builder::addCompilerPluginData)

        return builder
    }

    private fun FirFunction.memberKind(): MemberKind {
        return when (origin) {
            FirDeclarationOrigin.Delegated -> MemberKind.DELEGATION
            is FirDeclarationOrigin.Synthetic -> MemberKind.SYNTHESIZED
            else -> MemberKind.DECLARATION
        }
    }

    private fun shouldSetStableParameterNames(function: FirFunction?): Boolean {
        return when {
            function?.hasStableParameterNames == true -> true
            // for backward compatibility with K1, remove this line to fix KT-4758
            function?.origin == FirDeclarationOrigin.Delegated -> true
            else -> false
        }
    }

    private fun typeAliasProto(typeAlias: FirTypeAlias): ProtoBuf.TypeAlias.Builder? = whileAnalysing<ProtoBuf.TypeAlias.Builder?>(
        session, typeAlias
    ) {
        val builder = ProtoBuf.TypeAlias.newBuilder()
        val local = createChildSerializer(typeAlias)

        val flags = Flags.getTypeAliasFlags(
            typeAlias.nonSourceAnnotations(session).isNotEmpty() || extension.hasAdditionalAnnotations(typeAlias),
            ProtoEnumFlags.visibility(normalizeVisibility(typeAlias))
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(typeAlias.name)

        for (typeParameter in typeAlias.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            builder.addTypeParameter(local.typeParameterProto(typeParameter))
        }

        val underlyingType = typeAlias.expandedConeType!!
        if (useTypeTable()) {
            builder.underlyingTypeId = local.typeId(underlyingType)
        } else {
            builder.setUnderlyingType(local.typeProto(underlyingType, abbreviationOnly = true))
        }

        val expandedType = underlyingType.fullyExpandedType()
        if (useTypeTable()) {
            builder.expandedTypeId = local.typeId(expandedType)
        } else {
            builder.setExpandedType(local.typeProto(expandedType))
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(typeAlias))
            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(
                    writeLanguageVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes, versionRequirementTable)
                )
            }
        }

        extension.serializeTypeAlias(typeAlias, builder)
        builder.serializeCompilerPluginMetadata(typeAlias, ProtoBuf.TypeAlias.Builder::addCompilerPluginData)

        return builder
    }

    private fun enumEntryProto(enumEntry: FirEnumEntry): ProtoBuf.EnumEntry.Builder = whileAnalysing(session, enumEntry) {
        val builder = ProtoBuf.EnumEntry.newBuilder()
        builder.name = getSimpleNameIndex(enumEntry.name)
        extension.serializeEnumEntry(enumEntry, builder)
        return builder
    }

    private fun constructorProto(constructor: FirConstructor): ProtoBuf.Constructor.Builder = whileAnalysing(session, constructor) {
        val builder = ProtoBuf.Constructor.newBuilder()

        val local = createChildSerializer(constructor)

        val flags = Flags.getConstructorFlags(
            constructor.nonSourceAnnotations(session).isNotEmpty() || extension.hasAdditionalAnnotations(constructor),
            ProtoEnumFlags.visibility(normalizeVisibility(constructor)),
            !constructor.isPrimary,
            shouldSetStableParameterNames(constructor),
            constructor.status.hasMustUseReturnValue
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        for ((index, valueParameter) in constructor.valueParameters.withIndex()) {
            builder.addValueParameter(local.valueParameterProto(valueParameter, index, constructor))
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(constructor))

            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes))
            }
        }

        extension.serializeConstructor(constructor, builder, local)
        builder.serializeCompilerPluginMetadata(constructor, ProtoBuf.Constructor.Builder::addCompilerPluginData)
        return builder
    }

    private fun valueParameterProto(
        parameter: FirValueParameter,
        index: Int,
        function: FirFunction,
        additionalAnnotations: List<FirAnnotation> = emptyList(),
    ): ProtoBuf.ValueParameter.Builder = whileAnalysing(session, parameter) {
        val declaresDefaultValue = if (
            stdLibCompilation &&
            function is FirConstructor &&
            function.returnTypeRef.coneType.classId == StandardClassIds.Enum
        ) {
            false
        } else {
            function.itOrExpectHasDefaultParameterValue(index)
        }

        return valueParameterProto(parameter, additionalAnnotations, declaresDefaultValue)
    }

    private fun valueParameterProto(
        parameter: FirValueParameter,
        additionalAnnotations: List<FirAnnotation>,
        declaresDefaultValue: Boolean,
    ): ProtoBuf.ValueParameter.Builder {
        val builder = ProtoBuf.ValueParameter.newBuilder()

        val flags = Flags.getValueParameterFlags(
            additionalAnnotations.isNotEmpty()
                    || parameter.nonSourceAnnotations(session).isNotEmpty()
                    || extension.hasAdditionalAnnotations(parameter),
            declaresDefaultValue,
            parameter.isCrossinline,
            parameter.isNoinline
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(parameter.name)

        if (useTypeTable()) {
            builder.typeId = typeId(parameter.returnTypeRef)
        } else {
            builder.setType(typeProto(parameter.returnTypeRef))
        }

        if (parameter.isVararg) {
            val delegatedTypeAttrs = (parameter.returnTypeRef as? FirResolvedTypeRef)?.delegatedTypeRef?.coneTypeOrNull?.attributes
            val varargElementType = parameter.returnTypeRef.coneType.varargElementType().applyIf(delegatedTypeAttrs != null) {
                withAttributes(delegatedTypeAttrs!!)
            }

            if (useTypeTable()) {
                builder.varargElementTypeId = typeId(varargElementType)
            } else {
                builder.setVarargElementType(typeProto(varargElementType))
            }
        }

        if (shouldWriteAnnotationParameterDefaultValues(extension.metadataVersion) &&
            parameter.containingDeclarationSymbol.isAnnotationConstructor(session)
        ) {
            parameter.defaultValue?.toConstantValue<ConstantValue<*>>(session, scopeSession, extension.constValueProvider)?.let { value ->
                builder.setAnnotationParameterDefaultValue(extension.annotationSerializer.valueProto(value))
            }
        }

        extension.serializeValueParameter(parameter, builder)

        return builder
    }

    private fun shouldWriteAnnotationParameterDefaultValues(version: BinaryVersion): Boolean =
        version.major > 2 || (version.major == 2 && version.minor >= 2)

    private fun typeParameterProto(typeParameter: FirTypeParameter): ProtoBuf.TypeParameter.Builder =
        whileAnalysing(session, typeParameter) {
            val builder = ProtoBuf.TypeParameter.newBuilder()

            builder.id = getTypeParameterId(typeParameter)

            builder.name = getSimpleNameIndex(typeParameter.name)

            if (typeParameter.isReified != builder.reified) {
                builder.reified = typeParameter.isReified
            }

            val variance = ProtoEnumFlags.variance(typeParameter.variance)
            if (variance != builder.variance) {
                builder.variance = variance
            }
            extension.serializeTypeParameter(typeParameter, builder)

            val upperBounds = typeParameter.bounds
            if (upperBounds.size == 1 && upperBounds.single() is FirImplicitNullableAnyTypeRef) return builder

            for (upperBound in upperBounds) {
                if (useTypeTable()) {
                    builder.addUpperBoundId(typeId(upperBound))
                } else {
                    builder.addUpperBound(typeProto(upperBound))
                }
            }

            return builder
        }

    fun typeId(typeRef: FirTypeRef, toSuper: Boolean = false): Int {
        if (typeRef !is FirResolvedTypeRef) {
            return -1 // TODO: serializeErrorType?
        }
        return typeId(typeRef.coneType, toSuper)
    }

    fun typeId(type: ConeKotlinType, toSuper: Boolean = false): Int = typeTable[typeProto(type, toSuper)]

    private fun typeProto(typeRef: FirTypeRef, toSuper: Boolean = false): ProtoBuf.Type.Builder {
        return typeProto(typeRef.coneType, toSuper, correspondingTypeRef = typeRef)
    }

    private fun typeProto(
        type: ConeKotlinType,
        toSuper: Boolean = false,
        correspondingTypeRef: FirTypeRef? = null,
        isDefinitelyNotNullType: Boolean = false,
        abbreviationOnly: Boolean = false,
    ): ProtoBuf.Type.Builder {
        // `type` we'll see here will be fully expanded with the declaration
        // site session, but [FirElementSerializer] will be run with a use site one,
        // so it must be expanded twice.
        val useSiteSessionExpandedType = type.fullyExpandedType()
        val abbreviation = type.abbreviatedTypeOrSelf
        val mainType = if (!abbreviationOnly) useSiteSessionExpandedType else abbreviation
        val mainTypeProto = typeOrTypealiasProto(
            mainType, toSuper, correspondingTypeRef, isDefinitelyNotNullType,
            abbreviationOnly = abbreviationOnly,
        )

        // `typeOrTypealiasProto()` calls may in turn call `typeProto()`
        // for some other types in such a way that those types share the same
        // AbbreviatedType attribute (this happens, for example, with aliases
        // to suspend function types).
        // So, the abbreviated type may have been set already by an inner call.
        // Note that in case of an `expect typealias`, the second expansion will
        // erase `AbbreviatedTypeAttribute` of `type`.
        if (abbreviation != mainType && !mainTypeProto.hasAbbreviatedType() && !abbreviation.containsCapturedTypes) {
            val abbreviationProto = typeOrTypealiasProto(
                abbreviation, toSuper, correspondingTypeRef, isDefinitelyNotNullType,
                abbreviationOnly = false,
                isAbbreviation = true,
            )

            if (useTypeTable()) {
                mainTypeProto.abbreviatedTypeId = typeTable[abbreviationProto]
            } else {
                mainTypeProto.setAbbreviatedType(abbreviationProto)
            }
        }

        return mainTypeProto
    }

    private val ConeKotlinType.containsCapturedTypes get() = contains { it is ConeCapturedType }

    private fun typeOrTypealiasProto(
        type: ConeKotlinType,
        toSuper: Boolean,
        correspondingTypeRef: FirTypeRef?,
        isDefinitelyNotNullType: Boolean,
        isAbbreviation: Boolean = false,
        abbreviationOnly: Boolean = false,
    ): ProtoBuf.Type.Builder {
        val builder = ProtoBuf.Type.newBuilder()
        val typeAnnotations = mutableListOf<FirAnnotation>()
        when (type) {
            is ConeDefinitelyNotNullType -> return typeProto(type.original, toSuper, correspondingTypeRef, isDefinitelyNotNullType = true)
            is ConeErrorType -> {
                extension.serializeErrorType(type, builder)
                return builder
            }
            is ConeFlexibleType -> {
                val lowerBound = typeProto(type.lowerBound)
                val upperBound = typeProto(type.upperBound)
                extension.serializeFlexibleType(type, lowerBound, upperBound)
                if (useTypeTable()) {
                    lowerBound.flexibleUpperBoundId = typeTable[upperBound]
                } else {
                    lowerBound.setFlexibleUpperBound(upperBound)
                }
                return lowerBound
            }
            is ConeClassLikeType -> {
                val functionTypeKind = type.functionTypeKind(session)
                if (!isAbbreviation && functionTypeKind == FunctionTypeKind.SuspendFunction) {
                    val runtimeFunctionType = type.suspendFunctionTypeToFunctionTypeWithContinuation(
                        session, StandardClassIds.Continuation
                    )
                    val functionType = typeProto(runtimeFunctionType)
                    functionType.flags = Flags.getTypeFlags(true, false)
                    return functionType
                }
                if (!isAbbreviation && functionTypeKind?.isBuiltin == false) {
                    val legacySerializationUntil =
                        LanguageVersion.fromVersionString(functionTypeKind.serializeAsFunctionWithAnnotationUntil)
                    if (legacySerializationUntil != null && languageVersionSettings.languageVersion < legacySerializationUntil) {
                        return typeProto(type.customFunctionTypeToSimpleFunctionType(session))
                    }
                }
                fillFromPossiblyInnerType(builder, type, abbreviationOnly)
                if (type.hasContextParameters) {
                    typeAnnotations.addIfNotNull(
                        createAnnotationFromAttribute(
                            correspondingTypeRef?.annotations, CompilerConeAttributes.ContextFunctionTypeParams.ANNOTATION_CLASS_ID,
                            argumentMapping = buildAnnotationArgumentMapping {
                                this.mapping[StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME] =
                                    buildLiteralExpression(
                                        source = null, ConstantValueKind.Int, type.contextParameterNumberForFunctionType, setType = true
                                    )
                            }
                        )
                    )
                }
            }
            is ConeTypeParameterType -> {
                val typeParameter = type.lookupTag.typeParameterSymbol.fir
                if (typeParameter in ((currentDeclaration as? FirMemberDeclaration)?.typeParameters ?: emptyList())) {
                    builder.typeParameterName = getSimpleNameIndex(typeParameter.name)
                } else {
                    builder.typeParameter = getTypeParameterId(typeParameter)
                }

                if (isDefinitelyNotNullType) {
                    metDefinitelyNotNullType = true
                    builder.flags = Flags.getTypeFlags(false, isDefinitelyNotNullType)
                }
            }
            is ConeIntersectionType -> {
                val approximatedType = if (toSuper) {
                    typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes)
                } else {
                    typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes)
                }
                assert(approximatedType != type && approximatedType is ConeKotlinType) {
                    "Approximation failed: ${type.renderForDebugging()}"
                }
                return typeProto(approximatedType as ConeKotlinType)
            }
            is ConeIntegerLiteralType -> {
                throw IllegalStateException("Integer literal types should not persist up to the serializer: ${type.renderForDebugging()}")
            }
            is ConeCapturedType -> {
                throw IllegalStateException("Captured types should not persist up to the serializer: ${type.renderForDebugging()}")
            }
            else -> {
                throw AssertionError("Should not be here: ${type::class.java}")
            }
        }

        if (type.isMarkedNullable != builder.nullable) {
            builder.nullable = type.isMarkedNullable
        }

        val extensionAttributes = mutableListOf<ConeAttribute<*>>()
        // KT-67474: In K1, iteration order of `type.attributes` is the following: 1) custom attrs, then 2) builtin attrs;
        //   see it in `Annotations.withExtensionFunctionAnnotation` in functionTypes.kt
        // In K2, relevant iteration order of ArrayMap is defined by order of registration, defined by initialization order of the following properties:
        // - `ConeAttributes.WithExtensionFunctionType` in ConeAttributes.kt and
        // - `ConeAttributes.custom` in CustomAnnotationTypeAttribute.kt
        // To put custom attrs before builtin ones, as K1 does, the following partial sorting is used.
        val sortedAttributes = type.attributes.sortedBy { it !is CustomAnnotationTypeAttribute }
        for (attribute in sortedAttributes) {
            when {
                attribute is CustomAnnotationTypeAttribute -> typeAnnotations.addAll(attribute.annotations.nonSourceAnnotations(session))
                attribute is ParameterNameTypeAttribute -> typeAnnotations.addAll(attribute.annotations.nonSourceAnnotations(session))
                attribute.key in CompilerConeAttributes.classIdByCompilerAttributeKey ->
                    typeAnnotations.addIfNotNull(createAnnotationForCompilerDefinedTypeAttribute(attribute))
                else -> extensionAttributes += attribute
            }
        }

        for (attributeExtension in session.extensionService.typeAttributeExtensions) {
            for (attribute in extensionAttributes) {
                typeAnnotations.addIfNotNull(attributeExtension.convertAttributeToAnnotation(attribute))
            }
        }

        extension.serializeTypeAnnotations(typeAnnotations, builder)
        return builder
    }

    private fun createAnnotationForCompilerDefinedTypeAttribute(attribute: ConeAttribute<*>): FirAnnotation? {
        val lookupTag = CompilerConeAttributes.classIdByCompilerAttributeKey.getValue(attribute.key).toLookupTag()
        val annotationClassSymbol = lookupTag.toRegularClassSymbol(session)
        if (annotationClassSymbol?.getRetention(session) == AnnotationRetention.SOURCE) return null
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                this.coneType = ConeClassLikeTypeImpl(
                    lookupTag,
                    emptyArray(),
                    isMarkedNullable = false
                )
            }
            argumentMapping = FirEmptyAnnotationArgumentMapping
        }
    }

    private fun createAnnotationFromAttribute(
        existingAnnotations: List<FirAnnotation>?,
        classId: ClassId,
        argumentMapping: FirAnnotationArgumentMapping = FirEmptyAnnotationArgumentMapping,
    ): FirAnnotation? {
        return runIf(existingAnnotations?.any { it.annotationTypeRef.coneType.classId == classId } != true) {
            buildAnnotation {
                annotationTypeRef = buildResolvedTypeRef {
                    this.coneType = classId.constructClassLikeType(
                        emptyArray(), isMarkedNullable = false
                    )
                }
                this.argumentMapping = argumentMapping
            }
        }
    }

    private fun fillFromPossiblyInnerType(
        builder: ProtoBuf.Type.Builder,
        symbol: FirClassLikeSymbol<*>,
        typeArguments: Array<out ConeTypeProjection>,
        typeArgumentIndex: Int,
        abbreviationOnly: Boolean = false,
    ) {
        var argumentIndex = typeArgumentIndex
        val classifier = symbol.fir
        val classifierId = getClassifierId(classifier)
        if (classifier is FirTypeAlias) {
            builder.typeAliasName = classifierId
        } else {
            builder.className = classifierId
        }
        for (i in 0 until classifier.typeParameters.size) {
            // Next type parameter is not for this type but for an outer type.
            if (classifier.typeParameters[i] !is FirTypeParameter) break
            // No explicit type argument provided. For example: `Map.Entry<K, V>` when we get to `Map`
            // it has type parameters, but no explicit type arguments are provided for it.
            if (argumentIndex >= typeArguments.size) return
            builder.addArgument(typeArgument(typeArguments[argumentIndex++], abbreviationOnly))
        }

        if (!symbol.isInner) return
        val outerClassId = symbol.classId.outerClassId
        if (outerClassId == null || outerClassId.isLocal) return
        val outerSymbol = outerClassId.toLookupTag().toSymbol(session)
        if (outerSymbol != null) {
            val outerBuilder = ProtoBuf.Type.newBuilder()
            fillFromPossiblyInnerType(outerBuilder, outerSymbol, typeArguments, argumentIndex)
            if (useTypeTable()) {
                builder.outerTypeId = typeTable[outerBuilder]
            } else {
                builder.setOuterType(outerBuilder)
            }
        }
    }

    private fun fillFromPossiblyInnerType(builder: ProtoBuf.Type.Builder, type: ConeClassLikeType, abbreviationOnly: Boolean = false) {
        val classifierSymbol = type.lookupTag.toSymbol(session)
        if (classifierSymbol != null) {
            fillFromPossiblyInnerType(builder, classifierSymbol, type.typeArguments, 0, abbreviationOnly)
        } else {
            builder.className = getClassifierId(type.lookupTag.classId)
            type.typeArguments.forEach { builder.addArgument(typeArgument(it, abbreviationOnly)) }
        }
    }

    private fun typeArgument(typeProjection: ConeTypeProjection, abbreviationOnly: Boolean = false): ProtoBuf.Type.Argument.Builder {
        val builder = ProtoBuf.Type.Argument.newBuilder()

        if (typeProjection is ConeStarProjection) {
            builder.projection = ProtoBuf.Type.Argument.Projection.STAR
        } else if (typeProjection is ConeKotlinTypeProjection) {
            val projection = ProtoEnumFlags.projection(typeProjection.kind)

            if (projection != builder.projection) {
                builder.projection = projection
            }

            if (useTypeTable()) {
                builder.typeId = typeId(typeProjection.type)
            } else {
                builder.setType(typeProto(typeProjection.type, abbreviationOnly = abbreviationOnly))
            }
        }

        return builder
    }

    private fun getAccessorFlags(accessor: FirPropertyAccessor, property: FirProperty): Int {
        return Flags.getAccessorFlags(
            accessor.nonSourceAnnotations(session).isNotEmpty() || extension.hasAdditionalAnnotations(accessor),
            ProtoEnumFlags.visibility(normalizeVisibility(accessor)),
            // non-default accessor modality is always final, so we check property.modality instead
            ProtoEnumFlags.modality(property.modality!!),
            !isDefaultAccessor(accessor, property),
            accessor.isExternal,
            accessor.isInline,
        )
    }

    private fun isDefaultAccessor(accessor: FirPropertyAccessor, property: FirProperty): Boolean {
        if (property.isLocalInFunction) return true

        // [FirDefaultPropertyAccessor]---a property accessor without body---can still hold other information, such as annotations,
        // user-contributed visibility, and modifiers, such as `external` or `inline`.
        val hasSetterParameterAnnotations = accessor.valueParameters.any { setterParameter ->
            setterParameter.nonSourceAnnotations(session).isNotEmpty() || extension.hasAdditionalAnnotations(setterParameter)
        }
        return accessor is FirDefaultPropertyAccessor &&
                !hasSetterParameterAnnotations &&
                accessor.visibility == property.visibility &&
                !accessor.isExternal &&
                !accessor.isInline
    }

    private fun createChildSerializer(declaration: FirDeclaration): FirElementSerializer =
        FirElementSerializer(
            session, scopeSession, declaration, Interner(typeParameters), extension,
            typeTable, versionRequirementTable, serializeTypeTableToFunction = false,
            typeApproximator, languageVersionSettings, produceHeaderKlib
        )

    val stringTable: FirElementAwareStringTable
        get() = extension.stringTable

    private fun useTypeTable(): Boolean = extension.shouldUseTypeTable()

    private fun MutableVersionRequirementTable.serializeVersionRequirements(container: FirAnnotationContainer): List<Int> =
        serializeVersionRequirements(container.annotations)

    private fun MutableVersionRequirementTable.serializeVersionRequirements(annotations: List<FirAnnotation>): List<Int> =
        annotations
            .filter {
                it.toAnnotationClassId(session)?.asSingleFqName() == RequireKotlinConstants.FQ_NAME
            }
            .mapNotNull(::serializeVersionRequirementFromRequireKotlin)
            .map(::get)

    private fun MutableVersionRequirementTable.writeVersionRequirement(languageFeature: LanguageFeature): Int {
        return writeLanguageVersionRequirement(languageFeature, this)
    }

    private fun serializeVersionRequirementFromRequireKotlin(annotation: FirAnnotation): ProtoBuf.VersionRequirement.Builder? {
        val convertedAnnotation = annotation.toConstantValue<AnnotationValue>(session, scopeSession, extension.constValueProvider) ?: return null
        val argumentMapping = convertedAnnotation.value.argumentsMapping

        val versionString = argumentMapping[RequireKotlinConstants.VERSION]?.value as String? ?: return null
        val matchResult = RequireKotlinConstants.VERSION_REGEX.matchEntire(versionString) ?: return null

        val major = matchResult.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = matchResult.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val patch = matchResult.groupValues.getOrNull(4)?.toIntOrNull() ?: 0

        val proto = ProtoBuf.VersionRequirement.newBuilder()
        VersionRequirement.Version(major, minor, patch).encode(
            writeVersion = { proto.version = it },
            writeVersionFull = { proto.versionFull = it }
        )

        val message = argumentMapping[RequireKotlinConstants.MESSAGE]?.value as String?
        if (message != null) {
            proto.message = stringTable.getStringIndex(message)
        }

        when ((argumentMapping[RequireKotlinConstants.LEVEL] as EnumValue?)?.enumEntryName?.asString()) {
            DeprecationLevel.ERROR.name -> {
                // ERROR is the default level
            }
            DeprecationLevel.WARNING.name -> proto.level = ProtoBuf.VersionRequirement.Level.WARNING
            DeprecationLevel.HIDDEN.name -> proto.level = ProtoBuf.VersionRequirement.Level.HIDDEN
        }

        when ((argumentMapping[RequireKotlinConstants.VERSION_KIND] as EnumValue?)?.enumEntryName?.asString()) {
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION.name -> {
                // LANGUAGE_VERSION is the default kind
            }
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
            ProtoBuf.VersionRequirement.VersionKind.API_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.API_VERSION
        }

        val errorCode = (argumentMapping[RequireKotlinConstants.ERROR_CODE] as? IntValue)?.value
        if (errorCode != null && errorCode != -1) {
            proto.errorCode = errorCode
        }

        return proto
    }

    private fun normalizeVisibility(declaration: FirMemberDeclaration): Visibility {
        // REPL snippet declarations are lowered to public ones, so we should save the metadata accordingly
        return if (declaration.isReplSnippetDeclaration == true) Visibilities.Public else declaration.visibility.normalize()
    }

    private fun normalizeVisibility(declaration: FirPropertyAccessor): Visibility {
        // REPL snippet declarations are lowered to public ones, so we should save the metadata accordingly
        return if (declaration.isReplSnippetDeclaration == true) Visibilities.Public else declaration.visibility.normalize()
    }

    private fun getClassifierId(declaration: FirClassLikeDeclaration): Int {
        declaration.containingScriptSymbolAttr?.let { return getScriptOrReplClassId(declaration, scriptClassId(it.fir)) }
        declaration.containingReplSymbolAttr?.let { return getScriptOrReplClassId(declaration, snippetClassId(it.fir)) }
        return stringTable.getFqNameIndex(declaration)
    }

    private fun getScriptOrReplClassId(declaration: FirClassLikeDeclaration, containerClassId: ClassId): Int {
        val relativeClassId = declaration.symbol.classId.relativeClassName.pathSegments()
            .fold(containerClassId) { acc, n -> acc.createNestedClassId(n) }
        return stringTable.getQualifiedClassNameIndex(relativeClassId)
    }

    private fun getClassifierId(classId: ClassId): Int =
        stringTable.getQualifiedClassNameIndex(classId)

    private fun getSimpleNameIndex(name: Name): Int =
        stringTable.getStringIndex(name.asString())

    private fun getTypeParameterId(typeParameter: FirTypeParameter): Int =
        typeParameters.intern(typeParameter)

    companion object {
        @JvmStatic
        fun createTopLevel(
            session: FirSession,
            scopeSession: ScopeSession,
            extension: FirSerializerExtension,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
            produceHeaderKlib: Boolean = false,
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, null,
                Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                serializeTypeTableToFunction = false,
                typeApproximator,
                languageVersionSettings,
                produceHeaderKlib,
            )

        @JvmStatic
        fun createForLambda(
            session: FirSession,
            scopeSession: ScopeSession,
            extension: FirSerializerExtension,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, null,
                Interner(), extension, MutableTypeTable(),
                versionRequirementTable = null, serializeTypeTableToFunction = true,
                typeApproximator,
                languageVersionSettings,
                produceHeaderKlib = false,
            )

        @JvmStatic
        fun create(
            session: FirSession,
            scopeSession: ScopeSession,
            klass: FirClass,
            extension: FirSerializerExtension,
            parentSerializer: FirElementSerializer?,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
            produceHeaderKlib: Boolean = false,
        ): FirElementSerializer {
            val parentClassId = klass.symbol.classId.outerClassId
            val parent = if (parentClassId != null && !parentClassId.isLocal) {
                val parentClass = session.symbolProvider.getClassLikeSymbolByClassId(parentClassId)!!.fir as FirRegularClass
                parentSerializer ?: create(
                    session, scopeSession, parentClass, extension, null, typeApproximator,
                    languageVersionSettings, produceHeaderKlib
                )
            } else {
                createTopLevel(session, scopeSession, extension, typeApproximator, languageVersionSettings, produceHeaderKlib)
            }

            // Calculate type parameter ids for the outer class beforehand, as it would've had happened if we were always
            // serializing outer classes before nested classes.
            // Otherwise our interner can get wrong ids because we may serialize classes in any order.
            val serializer = FirElementSerializer(
                session,
                scopeSession,
                klass,
                Interner(parent.typeParameters),
                extension,
                MutableTypeTable(),
                if (parentClassId != null && !isKotlin1Dot4OrLater(extension.metadataVersion)) {
                    parent.versionRequirementTable
                } else {
                    MutableVersionRequirementTable()
                },
                serializeTypeTableToFunction = false,
                typeApproximator,
                languageVersionSettings,
                produceHeaderKlib,
            )
            for (typeParameter in klass.typeParameters) {
                if (typeParameter !is FirTypeParameter) continue
                serializer.typeParameters.intern(typeParameter)
            }
            return serializer
        }

        @JvmStatic
        fun createForScript(
            session: FirSession,
            scopeSession: ScopeSession,
            script: FirScript,
            extension: FirSerializerExtension,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
            produceHeaderKlib: Boolean = false,
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, script,
                Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                serializeTypeTableToFunction = false,
                typeApproximator,
                languageVersionSettings,
                produceHeaderKlib,
            )

        @JvmStatic
        fun createForSnippet(
            session: FirSession,
            scopeSession: ScopeSession,
            snippet: FirReplSnippet,
            extension: FirSerializerExtension,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
            produceHeaderKlib: Boolean = false,
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, snippet,
                Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                serializeTypeTableToFunction = false,
                typeApproximator,
                languageVersionSettings,
                produceHeaderKlib,
            )

        private fun writeLanguageVersionRequirement(
            languageFeature: LanguageFeature,
            versionRequirementTable: MutableVersionRequirementTable
        ): Int {
            val languageVersion = languageFeature.sinceVersion!!
            return writeVersionRequirement(
                languageVersion.major, languageVersion.minor, 0,
                ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION,
                versionRequirementTable
            )
        }

        private fun writeVersionRequirement(
            major: Int,
            minor: Int,
            patch: Int,
            versionKind: ProtoBuf.VersionRequirement.VersionKind,
            versionRequirementTable: MutableVersionRequirementTable
        ): Int {
            val requirement = ProtoBuf.VersionRequirement.newBuilder().apply {
                VersionRequirement.Version(major, minor, patch).encode(
                    writeVersion = { version = it },
                    writeVersionFull = { versionFull = it }
                )
                if (versionKind != defaultInstanceForType.versionKind) {
                    this.versionKind = versionKind
                }
            }
            return versionRequirementTable[requirement]
        }
    }

    private inline fun <M : GeneratedMessageLite.ExtendableMessage<M>, B : GeneratedMessageLite.Builder<M, B>> B.serializeCompilerPluginMetadata(
        declaration: FirDeclaration,
        addCompilerPluginData: B.(ProtoBuf.CompilerPluginData.Builder) -> B
    ) {
        extension.additionalMetadataProvider?.findMetadataExtensionsFor(declaration)?.forEach { (pluginId, data) ->
            val pluginData = ProtoBuf.CompilerPluginData.newBuilder().apply {
                this.pluginId = stringTable.getStringIndex(pluginId)
                this.data = ByteString.copyFrom(data)
            }
            addCompilerPluginData(pluginData)
        }
    }
}

internal fun scriptClassId(script: FirScript): ClassId =
    ClassId(script.symbol.fqName.parentOrNull() ?: FqName.ROOT, NameUtils.getScriptTargetClassName(script.name))

internal fun snippetClassId(snippet: FirReplSnippet): ClassId =
    ClassId(FqName.ROOT, NameUtils.getSnippetTargetClassName(snippet.name))

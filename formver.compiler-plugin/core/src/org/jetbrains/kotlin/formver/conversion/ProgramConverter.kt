/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.*
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.callables.*
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.embeddings.properties.*
import org.jetbrains.kotlin.formver.embeddings.types.*
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(val session: FirSession, override val config: PluginConfiguration, override val errorCollector: ErrorCollector) :
    ProgramConversionContext {
    private val methods: MutableMap<MangledName, FunctionEmbedding> =
        buildMap {
            putAll(SpecialKotlinFunctions.byName)
            putAll(PartiallySpecialKotlinFunctions.generateAllByName())
        }.toMutableMap()
    private val classes: MutableMap<MangledName, ClassTypeEmbedding> = mutableMapOf()
    private val properties: MutableMap<MangledName, PropertyEmbedding> = mutableMapOf()
    private val fields: MutableSet<FieldEmbedding> = mutableSetOf()

    // Cast is valid since we check that values are not null. We specify the type for `filterValues` explicitly to ensure there's no
    // loss of type information earlier.
    @Suppress("UNCHECKED_CAST")
    val debugExpEmbeddings: Map<MangledName, ExpEmbedding>
        get() = methods
            .mapValues { (it.value as? UserFunctionEmbedding)?.body?.debugExpEmbedding }
            .filterValues { value: ExpEmbedding? -> value != null } as Map<MangledName, ExpEmbedding>

    override val whileIndexProducer = indexProducer()
    override val catchLabelNameProducer = simpleFreshEntityProducer(::CatchLabelName)
    override val tryExitLabelNameProducer = simpleFreshEntityProducer(::TryExitLabelName)
    override val scopeIndexProducer = scopeIndexProducer()

    // The type annotation is necessary for the code to compile.
    override val anonVarProducer = FreshEntityProducer(::AnonymousVariableEmbedding)
    override val returnTargetProducer = FreshEntityProducer(::ReturnTarget)

    val program: Program
        get() = Program(
            domains = listOf(RuntimeTypeDomain(classes.values.toList())),
            // We need to deduplicate fields since public fields with the same name are represented differently
            // at `FieldEmbedding` level but map to the same Viper.
            fields = SpecialFields.all.map { it.toViper() } +
                    fields.distinctBy { it.name.mangled }.map { it.toViper() },
            functions = SpecialFunctions.all,
            methods = SpecialMethods.all + methods.values.mapNotNull { it.viperMethod }.distinctBy { it.name.mangled },
            predicates = classes.values.flatMap { listOf(it.details.sharedPredicate, it.details.uniquePredicate) }
        )

    fun registerForVerification(declaration: FirSimpleFunction) {
        val signature = embedFullSignature(declaration.symbol)
        val returnTarget = returnTargetProducer.getFresh(signature.callableType.returnType)
        val paramResolver =
            RootParameterResolver(
                this@ProgramConverter,
                signature,
                signature.symbol.valueParameterSymbols,
                signature.labelName,
                returnTarget,
            )
        val stmtCtx =
            MethodConverter(
                this@ProgramConverter,
                signature,
                paramResolver,
                scopeIndexProducer.getFresh(),
            ).statementCtxt()

        // Note: it is important that `body` is only set after `embedUserFunction` is complete, as we need to
        // place the embedding in the map before processing the body.
        embedUserFunction(declaration.symbol, signature).apply {
            body = stmtCtx.convertMethodWithBody(declaration, signature, returnTarget)
        }
    }

    fun embedUserFunction(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): UserFunctionEmbedding {
        (methods[signature.name] as? UserFunctionEmbedding)?.also { return it }
        val new = UserFunctionEmbedding(processCallable(symbol, signature))
        methods[signature.name] = new
        return new
    }

    private fun embedGetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedGetterName(this)
        return methods.getOrPut(name) {
            val signature = GetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    private fun embedSetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedSetterName(this)
        return methods.getOrPut(name) {
            val signature = SetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): FunctionEmbedding {
        val lookupName = symbol.embedName(this)
        return when (val existing = methods[lookupName]) {
            null -> {
                val signature = embedFullSignature(symbol)
                val callable = processCallable(symbol, signature)
                UserFunctionEmbedding(callable).also {
                    methods[lookupName] = it
                }
            }
            is PartiallySpecialKotlinFunction -> {
                if (existing.baseEmbedding != null)
                    return existing
                val signature = embedFullSignature(symbol)
                val callable = processCallable(symbol, signature)
                val userFunction = UserFunctionEmbedding(callable)
                existing.also {
                    it.initBaseEmbedding(userFunction)
                }
            }
            else -> existing
        }
    }

    /**
     * Returns an embedding of the class type, with details set.
     */
    private fun embedClass(symbol: FirRegularClassSymbol): ClassTypeEmbedding {
        val className = symbol.classId.embedName()
        val embedding = classes.getOrPut(className) {
            buildClassPretype {
                withName(className)
            }
        }
        if (embedding.hasDetails) return embedding

        val newDetails =
            ClassEmbeddingDetails(
                embedding,
                symbol.classKind.isInterface,
            )
        embedding.initDetails(newDetails)

        // The full class embedding is necessary to process the signatures of the properties of the class, since
        // these take the class as a parameter. We thus do this in three phases:
        // 1. Initialise the supertypes (including running this whole four-step process on each)
        // 2. Initialise the fields
        // 3. Process the properties of the class.
        //
        // With respect to the embedding, each phase is pure by itself, and only updates the class embedding at the end.
        // This ensures the code never sees half-built supertype or field data. The phases can, however, modify the
        // `ProgramConverter`.

        // Phase 1
        newDetails.initSuperTypes(symbol.resolvedSuperTypes.map { embedType(it).pretype })

        // Phase 2
        val properties = symbol.propertySymbols
        newDetails.initFields(properties.mapNotNull { propertySymbol ->
            SpecialProperties.isSpecial(propertySymbol).ifFalse {
                processBackingField(propertySymbol, symbol)
            }
        }.toMap())

        // Phase 3
        properties.forEach { processProperty(it, newDetails) }

        return embedding
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = buildType { embedTypeWithBuilder(type) }

    // Note: keep in mind that this function is necessary to resolve the name of the function!
    override fun embedFunctionPretype(symbol: FirFunctionSymbol<*>): FunctionTypeEmbedding = buildFunctionPretype {
        embedFunctionPretypeWithBuilder(symbol)
    }

    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding = if (symbol.isExtension) {
        embedCustomProperty(symbol)
    } else {
        // Ensure that the class has been processed.
        embedType(symbol.dispatchReceiverType!!)
        properties.getOrPut(symbol.embedMemberPropertyName()) {
            check(symbol is FirIntersectionOverridePropertySymbol) {
                "Unknown property ${symbol.callableId}."
            }
            embedCustomProperty(symbol)
        }
    }

    private fun <R> FirPropertySymbol.withConstructorParam(action: FirPropertySymbol.(FirValueParameterSymbol) -> R): R? =
        correspondingValueParameterFromPrimaryConstructor?.let { param ->
            action(param)
        }

    private fun extractConstructorParamsAsFields(symbol: FirFunctionSymbol<*>): Map<FirValueParameterSymbol, FieldEmbedding> {
        if (symbol !is FirConstructorSymbol || !symbol.isPrimary)
            return emptyMap()
        val constructedClassSymbol = symbol.resolvedReturnType.toRegularClassSymbol(session) ?: return emptyMap()
        val constructedClass = embedClass(constructedClassSymbol)

        return constructedClassSymbol.propertySymbols.mapNotNull { propertySymbol ->
            propertySymbol.withConstructorParam { paramSymbol ->
                constructedClass.details.findField(callableId.embedUnscopedPropertyName())?.let { paramSymbol to it }
            }
        }.toMap()
    }

    override fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): FunctionSignature {
        val dispatchReceiverType = symbol.receiverType
        val extensionReceiverType = symbol.extensionReceiverType
        val isExtensionReceiverUnique = symbol.receiverParameterSymbol?.isUnique(session) ?: false
        val isExtensionReceiverBorrowed = symbol.receiverParameterSymbol?.isBorrowed(session) ?: false
        return object : FunctionSignature {
            override val callableType: FunctionTypeEmbedding = embedFunctionPretype(symbol)

            // TODO: figure out whether we want a symbol here and how to get it.
            override val dispatchReceiver = dispatchReceiverType?.let {
                PlaceholderVariableEmbedding(
                    DispatchReceiverName,
                    embedType(it),
                    isUnique = false,
                    isBorrowed = false,
                )
            }

            override val extensionReceiver = extensionReceiverType?.let {
                PlaceholderVariableEmbedding(
                    ExtensionReceiverName,
                    embedType(it),
                    isExtensionReceiverUnique,
                    isExtensionReceiverBorrowed,
                )
            }

            override val params = symbol.valueParameterSymbols.map {
                FirVariableEmbedding(it.embedName(), embedType(it.resolvedReturnType), it, it.isUnique(session), it.isBorrowed(session))
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private val FirRegularClassSymbol.propertySymbols: List<FirPropertySymbol>
        get() {
            val result = mutableListOf<FirPropertySymbol>()
            this.fir.processAllDeclarations(session) {
                if (it is FirPropertySymbol) result.add(it)
            }
            return result
        }

    private fun embedFullSignature(symbol: FirFunctionSymbol<*>): FullNamedFunctionSignature {
        val subSignature = object : NamedFunctionSignature, FunctionSignature by embedFunctionSignature(symbol) {
            override val name = symbol.embedName(this@ProgramConverter)
            override val labelName: String
                get() = super<NamedFunctionSignature>.labelName
            override val symbol = symbol
        }
        val constructorParamSymbolsToFields = extractConstructorParamsAsFields(symbol)
        val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, subSignature)

        @OptIn(SymbolInternals::class)
        val declaration = symbol.fir
        val body = declaration.body

        /** Specifications are only allowed inside simple functions.
         * We are also unable to retrieve them when body is not visible,
         * although ideally we should be able to see preconditions and postconditions
         * from other modules.
         */
        val firSpec = when {
            declaration !is FirSimpleFunction -> null
            body == null -> null
            else -> extractFirSpecification(body, declaration.symbol.resolvedReturnType)
        }

        val rootResolver =
            RootParameterResolver(
                this@ProgramConverter,
                subSignature,
                subSignature.symbol.valueParameterSymbols,
                subSignature.labelName,
                ReturnTarget(depth = 0, subSignature.callableType.returnType),
            )

        fun createCtx(returnVariable: VariableEmbedding? = null): StmtConversionContext {
            val returnVarCtx = returnVariable?.let { ret -> firSpec?.returnVar?.let { ReturnVarSubstitutor(it, ret) } }
            val paramResolver =
                if (returnVarCtx != null)
                    SubstitutedReturnParameterResolver(rootResolver, returnVarCtx)
                else
                    rootResolver

            return MethodConverter(
                this@ProgramConverter,
                subSignature,
                paramResolver,
                scopeDepth = ScopeIndex.NoScope,
            ).statementCtxt()
        }

        return object : FullNamedFunctionSignature, NamedFunctionSignature by subSignature {
            // TODO (inhale vs require) Decide if `predicateAccessInvariant` should be required rather than inhaled in the beginning of the body.
            override fun getPreconditions() = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.pureInvariants())
                    addAll(it.accessInvariants())
                    if (it.isUnique) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant()?.fillHole(it))
                    }
                }
                addAll(subSignature.stdLibPreconditions())
                // We create a separate context to embed the preconditions here.
                // Hence, some parts of FIR are translated later than the other and less explicitly.
                // TODO: this process should be a separate step in the conversion.
                firSpec?.precond?.let {
                    addAll(createCtx().collectInvariants(it))
                }
            }

            override fun getPostconditions(returnVariable: VariableEmbedding) = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.accessInvariants())
                    if (it.isUnique && it.isBorrowed) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant()?.fillHole(it))
                    }
                }
                addAll(returnVariable.pureInvariants())
                addAll(returnVariable.provenInvariants())
                addAll(returnVariable.allAccessInvariants())
                if (subSignature.callableType.returnsUnique) {
                    addIfNotNull(returnVariable.uniquePredicateAccessInvariant())
                }
                addAll(contractVisitor.getPostconditions(ContractVisitorContext(returnVariable, symbol)))
                addAll(subSignature.stdLibPostconditions(returnVariable))
                addIfNotNull(primaryConstructorInvariants(returnVariable))
                // TODO: this process should be a separate step in the conversion (see above)
                firSpec?.postcond?.let {
                    addAll(createCtx(returnVariable).collectInvariants(it))
                }
            }

            fun primaryConstructorInvariants(returnVariable: VariableEmbedding): ExpEmbedding? {
                val invariants = params.mapNotNull { param ->
                    require(param is FirVariableEmbedding) { "Constructor parameters must be represented by FirVariableEmbeddings" }
                    constructorParamSymbolsToFields[param.symbol]?.let { field ->
                        (field.accessPolicy == AccessPolicy.ALWAYS_READABLE).ifTrue {
                            EqCmp(FieldAccess(returnVariable, field), param)
                        }
                    }
                }
                return if (invariants.isEmpty()) null
                else invariants.toConjunction()
            }

            override val declarationSource: KtSourceElement? = symbol.source
        }
    }

    private val FirFunctionSymbol<*>.containingPropertyOrSelf
        get() = when (this) {
            is FirPropertyAccessorSymbol -> propertySymbol
            else -> this
        }

    private val FirFunctionSymbol<*>.receiverType: ConeKotlinType?
        get() = containingPropertyOrSelf.dispatchReceiverType

    private val FirFunctionSymbol<*>.extensionReceiverType: ConeKotlinType?
        get() = containingPropertyOrSelf.resolvedReceiverTypeRef?.coneType

    /**
     * Construct and register the field embedding for this property's backing field, if any exists.
     */
    private fun processBackingField(
        symbol: FirPropertySymbol,
        classSymbol: FirRegularClassSymbol,
    ): Pair<SimpleKotlinName, FieldEmbedding>? {
        val embedding = embedClass(classSymbol)
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        val scopedName = symbol.callableId.embedMemberBackingFieldName(
            Visibilities.isPrivate(symbol.visibility)
        )
        val fieldIsAllowed = symbol.hasBackingField
                && !symbol.isCustom
                && (symbol.isFinal || classSymbol.isFinal)
        val backingField = scopedName.specialEmbedding(embedding) ?: fieldIsAllowed.ifTrue {
            UserFieldEmbedding(
                scopedName,
                embedType(symbol.resolvedReturnType),
                symbol,
                symbol.isUnique(session),
                embedding,
            )
        }
        return backingField?.let { unscopedName to it }
    }

    /**
     * Construct and register the property embedding (i.e. getter + setter) for this property.
     *
     * Note that the property either has associated Viper field (and then it is used to access the value) or not (in this case methods are used).
     * The field is only used for final properties with default getter and default setter (if any).
     *
     * Null value of parameter [embedding] means that there is no class details corresponding to this type (e.g. it is primitive).
     */
    private fun processProperty(symbol: FirPropertySymbol, embedding: ClassEmbeddingDetails?) {
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        properties[symbol.embedMemberPropertyName()] = SpecialProperties.byCallableId[symbol.callableId] ?: embedding.run {
            val backingField = embedding?.findField(unscopedName)
            backingField?.let { fields.add(it) }
            embedProperty(symbol, backingField)
        }
    }

    private fun embedCustomProperty(symbol: FirPropertySymbol) = embedProperty(symbol, null)

    private fun embedProperty(symbol: FirPropertySymbol, backingField: FieldEmbedding?) =
        PropertyEmbedding(
            embedGetter(symbol, backingField),
            symbol.isVar.ifTrue { embedSetter(symbol, backingField) },
        )

    private fun embedGetter(symbol: FirPropertySymbol, backingField: FieldEmbedding?): GetterEmbedding =
        if (backingField != null) {
            BackingFieldGetter(backingField)
        } else {
            CustomGetter(embedGetterFunction(symbol))
        }

    private fun embedSetter(symbol: FirPropertySymbol, backingField: FieldEmbedding?): SetterEmbedding =
        if (backingField != null) {
            BackingFieldSetter(backingField)
        } else {
            CustomSetter(embedSetterFunction(symbol))
        }

    @OptIn(SymbolInternals::class)
    private fun processCallable(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): RichCallableEmbedding {
        return if (symbol.shouldBeInlined) {
            InlineNamedFunction(signature, symbol.fir.body!!)
        } else {
            // We generate a dummy method header here to ensure all required types are processed already. If we skip this, any types
            // that are used only in contracts cause an error because they are not processed until too late.
            // TODO: fit this into the flow in some logical way instead.
            NonInlineNamedFunction(signature).also { it.toViperMethodHeader() }
        }
    }

    private fun TypeBuilder.embedTypeWithBuilder(type: ConeKotlinType): PretypeBuilder = when {
        type is ConeErrorType -> error("Encountered an erroneous type: $type")
        type is ConeTypeParameterType -> {
            isNullable = true; any()
        }
        type.isString -> {
            val stringClassSymbol = type.toClassSymbol(session) as FirRegularClassSymbol
            stringClassSymbol.propertySymbols.forEach {
                processProperty(it, embedding = null)
            }
            string()
        }
        type.isUnit -> unit()
        type.isChar -> char()
        type.isInt -> int()
        type.isBoolean -> boolean()
        type.isNothing -> nothing()
        type.isSomeFunctionType(session) -> function {
            check(type is ConeClassLikeType) { "Expected a ConeClassLikeType for a function type, got $type" }
            type.receiverType(session)?.let { withDispatchReceiver { embedTypeWithBuilder(it) } }
            type.valueParameterTypesWithoutReceivers(session).forEach { param ->
                withParam { embedTypeWithBuilder(param) }
            }
            withReturnType { embedTypeWithBuilder(type.returnType(session)) }
        }
        type.canBeNull(session) -> {
            isNullable = true
            embedTypeWithBuilder(type.withNullability(false, session.typeContext))
        }
        type.isAny -> any()
        type is ConeClassLikeType -> {
            val classLikeSymbol = type.toClassSymbol(session)
            if (classLikeSymbol is FirRegularClassSymbol) {
                existing(embedClass(classLikeSymbol))
            } else {
                unimplementedTypeEmbedding(type)
            }
        }
        else -> unimplementedTypeEmbedding(type)
    }

    private fun FunctionPretypeBuilder.embedFunctionPretypeWithBuilder(symbol: FirFunctionSymbol<*>) {
        symbol.receiverType?.let {
            withDispatchReceiver { embedTypeWithBuilder(it) }
        }
        symbol.extensionReceiverType?.let {
            withExtensionReceiver { embedTypeWithBuilder(it) }
        }
        symbol.valueParameterSymbols.forEach { param ->
            withParam {
                embedTypeWithBuilder(param.resolvedReturnType)
            }
        }
        withReturnType { embedTypeWithBuilder(symbol.resolvedReturnType) }
        returnsUnique = symbol.isUnique(session) || symbol is FirConstructorSymbol
    }

    private fun TypeBuilder.unimplementedTypeEmbedding(type: ConeKotlinType): PretypeBuilder =
        when (config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                throw NotImplementedError("The embedding for type $type is not yet implemented.")
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                errorCollector.addMinorError("Requested type $type, for which we do not yet have an embedding.")
                unit()
            }
        }
}

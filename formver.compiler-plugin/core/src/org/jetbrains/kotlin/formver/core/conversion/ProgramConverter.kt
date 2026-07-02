/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.*
import org.jetbrains.kotlin.formver.core.diagnostics.ConversionErrors
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.callables.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousBuiltinVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.*
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.core.purity.checkValidity
import org.jetbrains.kotlin.formver.core.purity.isPure
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue


/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(
    override val session: FirSession,
    override val config: PluginConfiguration,
    private val diagnosticContext: DiagnosticContext,
    private val reporter: DiagnosticReporter,
) : ProgramConversionContext {

    /**
     * Whether any primary diagnostic has been reported during conversion.
     *
     * Excludes the derived [ConversionErrors.VERIFICATION_SKIPPED] summary, which is itself fired
     * in reaction to primary diagnostics.
     */
    var hadConversionError: Boolean = false
        private set

    /** Source attached to source-less diagnostics (e.g. unimplemented features raised deep in conversion). */
    private var currentDeclarationSource: KtSourceElement? = null

    private fun emit(source: KtSourceElement?, factory: KtDiagnosticFactory1<String>, msg: String) {
        context(diagnosticContext) {
            reporter.reportOn(source, factory, msg)
        }
        hadConversionError = true
    }

    override fun reportPurityViolation(source: KtSourceElement?, msg: String) =
        emit(source, ConversionErrors.PURITY_VIOLATION, msg)

    override fun reportMinorInternalError(msg: String) =
        emit(currentDeclarationSource, ConversionErrors.MINOR_INTERNAL_ERROR, msg)

    private fun reportVerificationSkipped(source: KtSourceElement?, msg: String) {
        context(diagnosticContext) {
            reporter.reportOn(source, ConversionErrors.VERIFICATION_SKIPPED, msg)
        }
    }

    private val specialFunctions: Map<SymbolicName, SpecialKotlinFunction> = buildMap {
        putAll(SpecialKotlinFunctions.byName)
        putAll(PartiallySpecialKotlinFunctions.generateAllByName())
    }

    private val fullSignatures: MutableMap<SymbolicName, CompleteFunctionSignature> = mutableMapOf()

    private val callable: MutableMap<SymbolicName, SignatureWithTarget<NamedCallableEmbedding>> = mutableMapOf()

    private data class RegisteredFunction(
        val declaration: FirSimpleFunction,
        val signature: CompleteFunctionSignature,
        val returnTarget: ReturnTarget,
    )

    private val registered: MutableList<RegisteredFunction> = mutableListOf()

    override val typeResolver: TypeResolver = TypeResolver()

    private val typeEmbedder = TypeEmbedder(this)
    private val contractEmbedder = ContractEmbedder(this)

    val debugExpEmbeddings: Map<SymbolicName, ExpEmbedding>
        get() = buildMap {
            convertedBodyResolver.forEachImpure { name, body -> put(name, body.bodyExp) }
        }


    override val whileIndexProducer = indexProducer()
    override val catchLabelNameProducer = simpleFreshEntityProducer(::CatchLabelName)
    override val tryExitLabelNameProducer = simpleFreshEntityProducer(::TryExitLabelName)
    override val scopeIndexProducer = scopeIndexProducer()

    // The type annotation is necessary for the code to compile.
    override val anonVarProducer = FreshEntityProducer(::AnonymousVariableEmbedding)
    override val anonBuiltinVarProducer = FreshEntityProducer(::AnonymousBuiltinVariableEmbedding)
    override val returnTargetProducer = FreshEntityProducer(ReturnTarget::createForDepth)
    override val nameResolver = ShortNameResolver()
    override val convertedBodyResolver = ConvertedBodyResolver()
    override val linearizedBodyResolver = LinearizedBodyResolver()


    fun buildProgram(): Program = Program(
        domains = listOf(RuntimeTypeDomain(typeResolver)),
        // Public fields with the same name are represented differently at `FieldEmbedding` level
        // but map to the same Viper field, so we deduplicate before emitting.
        fields = typeResolver.backingFields().distinctBy { it.name }.map { it.toViper() },
        functions = SpecialFunctions.all + linearizedBodyResolver.functions,
        methods = SpecialMethods.all + linearizedBodyResolver.methods,
        predicates = typeResolver.classTypeEmbeddings().map {
            with(typeResolver) {
                it.uniquePredicate()
            }
        },
        adts = emptyList(),
    )


    // region Callable Conversion

    /**
     * Embed the declaration's signature and embeds the body.
     */
    fun register(declaration: FirSimpleFunction) {
        val signature = embedCompleteSignature(declaration.symbol)
        embedFunctionBody(declaration.symbol, signature)
        registered += RegisteredFunction(declaration, signature.signature, signature.returnTarget)
    }


    /**
     * Walk converted bodies to surface validity / purity errors via the diagnostic reporter. If any
     * conversion error has been reported by the time this returns, emit a [ConversionErrors.VERIFICATION_SKIPPED]
     * summary on every registered declaration so the user sees per-function attribution for the bail-out.
     * Callers should inspect [hadConversionError] afterwards and skip [linearizeAll] when set.
     */
    fun validateAll() {
        convertedBodyResolver.forEachImpure { name, body ->
            val source = fullSignatures[name]?.declarationSource
            body.bodyExp.checkValidity(source, this)
        }
        convertedBodyResolver.forEachPure { name, body ->
            if (!body.isPure()) {
                val source = fullSignatures[name]?.declarationSource
                reportPurityViolation(source, "Impure function body detected in pure function")
            }
        }
        if (hadConversionError) {
            for (entry in registered) {
                reportVerificationSkipped(
                    entry.declaration.source,
                    "Function '${entry.declaration.name.asString()}' was not verified because of errors in its declaration",
                )
            }
        }
    }


    private fun linearizePure(name: SymbolicName, signature: CompleteFunctionSignature) {
        val converted = convertedBodyResolver.lookupPure(name)
        val linearized = converted?.let { linearizePureBody(signature.declarationSource, it) }
        linearizedBodyResolver.storeFunction(name, signature.toViperFunction(typeResolver, linearized))
    }

    private fun linearizeImpure(name: SymbolicName, signature: CompleteFunctionSignature) {
        val source = signature.declarationSource
        val converted = convertedBodyResolver.lookupImpure(name)
        val method = if (converted != null) {
            linearizeImpureBody(source, converted).toViperMethod(signature, typeResolver)
        } else {
            signature.toViperMethod(typeResolver, null)
        }
        linearizedBodyResolver.storeMethod(name, method)
    }

    /**
     * Build the finalized Viper `Method` / `Function` for every embedded callable, storing each in
     * [linearizedBodyResolver]. Should only be invoked when [hadConversionError] is false.
     *
     * Iterates the embedding maps directly (rather than the converted-body map) so that callables
     * without a body still get a Viper header.
     */
    fun linearizeAll() {
        val (pure, impure) = fullSignatures.entries.partition { it.value.isPure }
        pure.forEach { linearizePure(it.key, it.value) }
        impure.forEach { linearizeImpure(it.key, it.value) }
    }

    // endregion

    // region Conversion Context

    private fun createBodyConversionContext(
        symbol: FirFunctionSymbol<*>, signature: SignatureWithTarget<NamedFunctionSignature>
    ): StmtConversionContext {
        val paramResolver = RootParameterResolver(
            this@ProgramConverter,
            signature.signature,
            symbol.valueParameterSymbols,
            signature.signature.labelName,
            signature.returnTarget
        )
        val stmtCtx = MethodConverter(
            this@ProgramConverter,
            signature.signature,
            paramResolver,
            scopeIndexProducer.getFresh(),
        ).statementCtxt()
        return stmtCtx
    }

    // endregion


    // region Functions

    /**
     * Embeds the body of a function. This must be called at most once per function.
     */
    @OptIn(SymbolInternals::class)
    private fun embedFunctionBody(
        symbol: FirFunctionSymbol<*>, signature: SignatureWithTarget<CompleteFunctionSignature>
    ) {
        val declaration = symbol.fir as? FirSimpleFunction ?: throw SnaktInternalException(
            symbol.source, "Expected FirSimpleFunction, got unexpected type ${symbol.fir.javaClass.simpleName}"
        )

        val context = createBodyConversionContext(symbol, signature)
        if (signature.signature.isPure) {
            val body = context.convertPureBody(declaration)
            convertedBodyResolver.storePure(signature.signature.name, body)
        } else {
            val body = context.convertImpureBody(declaration, signature.signature, signature.returnTarget)
            body?.let { convertedBodyResolver.storeImpure(signature.signature.name, it) }
        }
    }


    /**
     * Embeds the full function signature (with pre+post conditions).
     */
    private fun embedCompleteSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<CompleteFunctionSignature> {
        val callable = with(this) {
            val namedSignature = symbol.toFunctionSignature().toNamedSignature(symbol)
            if (symbol.shouldBeInlined) {
                namedSignature.toInlineSignature(symbol).also {
                    callable.putIfAbsent(it.signature.name, it)
                }
            } else {
                namedSignature.toNonInlineSignature(symbol).also {
                    callable.putIfAbsent(it.signature.name, it)
                }.toCompleteSignature(symbol)
            }
        }

        fullSignatures.putIfAbsent(callable.signature.name, callable.signature)

        return callable
    }

    /**
     * Returns the callable embedding if a matching special function exists.
     */
    fun embedSpecialFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding? {
        val name = symbol.embedName(this)
        return specialFunctions[name]?.also { existing ->
            if (existing !is PartiallySpecialKotlinFunction) return@also
            if (existing.baseEmbedding != null) return@also
            val signature = embedCompleteSignature(symbol)
            embedFunctionBody(symbol, signature)
            existing.initBaseEmbedding(signature.signature)
        }
    }

    /**
     * This function is the public interface to embed a function symbol.
     * If during the conversion of a function body a function call is made, then [embedAnyFunction] must be used to embed/lookup the function.
     *
     * It will return the callable embedding, if the function was seen the first time, it will embed the full function signature and body (if needed).
     */
    override fun embedAnyFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding {
        // NOTE: The order of the embedSpecialFunction and the callable lookup does matter.

        // check if it must be handled specially
        embedSpecialFunction(symbol)?.let { return it }

        // check if it already was embedded
        callable[symbol.embedName(this)]?.let { return it.signature }

        // first embed the complete signature
        val signature = embedCompleteSignature(symbol)

        // If the function is pure, we add the body.
        if (signature.signature.isPure && !symbol.neverConvert(session)) {
            embedFunctionBody(symbol, signature)
        }

        return signature.signature
    }

    // endregion


    // region contracts

    override fun embedContract(
        symbol: FirFunctionSymbol<*>, signature: NamedFunctionSignature, returnTarget: ReturnTarget
    ): Pair<List<ExpEmbedding>, List<ExpEmbedding>> = contractEmbedder.embedContract(symbol, signature, returnTarget)

    // endregion

    // region Properties

    /**
     * This function embeds properties. Depending on the behavior of the property, it can be handled differently.
     *
     *
     * * Closed `val` (No Getter): Maps to a pure function. This can be done because the value cannot be changed.
     * * Closed `val` (With Getter): Maps to an impure function to execute the custom getter logic.
     * * Closed `var`: Maps to a backing field. This value needs permissions and can be changed.
     * * Open `val` / `var`: Maps to an impure method. Abstracted because subclasses can override the behavior, introduce side effects, or turn a `val` into a `var`.
     * * Extension properties are handled like open properties.
     */
    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding {
        if (symbol.receiverParameterSymbol != null) {
            return embedExtensionProperty(symbol)
        } else {
            val name = symbol.embedMemberPropertyName(this)
            return typeResolver.getOrPutProperty(name) {
                // Check if the symbol should receive a special treatment
                with(typeResolver) {
                    with(session) {
                        SpecialProperties.lookup(symbol)?.let { return@getOrPutProperty it }
                    }
                }

                val classSymbol = symbol.dispatchReceiverType?.toClassSymbol(session)

                val regularClass = classSymbol as? FirRegularClassSymbol
                    ?: return@getOrPutProperty handleUnsupportedFeature(
                        symbol.source, "Properties dispatch receiver is not a regular class"
                    ) {
                        PropertyEmbedding(
                            getter = null,
                            setter = null,
                            hasDefaultBehaviour = false,
                            isUnique = false,
                            isVal = symbol.isVal,
                            type = embedType(symbol.resolvedReturnType),
                        )
                    }

                val isDefaultProperty = isGuaranteedDefaultProperty(symbol)
                val isImmutable = symbol.isVal
                val isManual = symbol.isManual(session)
                val isUnique = symbol.isUnique(session)

                val returnType = embedType(symbol.resolvedReturnType)

                val (getter, setter) = when {
                    (isDefaultProperty && !isImmutable) || isManual -> {
                        // use a backing field
                        val field = embedBackingField(symbol, regularClass)
                        Pair(BackingFieldGetter(field), BackingFieldSetter(field))
                    }

                    isDefaultProperty -> {
                        // use pure function
                        val functionType = buildFunctionPretype {
                            withDispatchReceiver(embedType(symbol.dispatchReceiverType!!))
                            withReturnType(returnType)
                        }
                        Pair(
                            CustomGetter(embedAccessorFunction(symbol, functionType, defaultBehaviour = true)), null
                        )
                    }

                    else -> {
                        // The property could be overridden by anything, or has a custom getter/setter.
                        // use impure function
                        val getterType = buildFunctionPretype {
                            withDispatchReceiver { any() }
                            withReturnType { nullableAny() }
                        }
                        val setterType = buildFunctionPretype {
                            withDispatchReceiver { any() }
                            withParam { nullableAny() }
                            withReturnType { nullableAny() }
                        }

                        Pair(
                            CustomGetter(embedAccessorFunction(symbol, getterType, defaultBehaviour = false)),
                            symbol.isVar.ifTrue {
                                CustomSetter(
                                    embedAccessorFunction(
                                        symbol, setterType, defaultBehaviour = false
                                    )
                                )
                            })
                    }
                }

                return@getOrPutProperty PropertyEmbedding(
                    getter, setter, isDefaultProperty || isManual, isUnique, isImmutable, returnType
                )

            }
        }
    }

    override fun isGuaranteedDefaultProperty(symbol: FirPropertySymbol): Boolean {
        val classSymbolFinal = symbol.dispatchReceiverType?.toClassSymbol(session)?.isFinal ?: false
        return (symbol.isFinal || classSymbolFinal) && !symbol.isCustom
    }

    /**
     * Construct and register the field embedding for this property's backing field, if any exists.
     */
    private fun embedBackingField(
        symbol: FirPropertySymbol, classSymbol: FirRegularClassSymbol
    ): FieldEmbedding {
        val embedding = typeEmbedder.embedClass(classSymbol)
        val scopedName = symbol.callableId!!.embedMemberBackingFieldName(scopePolicy(symbol, this))
        val backingField = UserFieldEmbedding(
            scopedName,
            embedType(symbol.resolvedReturnType),
            symbol,
            symbol.isUnique(session),
            embedding,
            symbol.isManual(session)
        )
        return backingField
    }

    private fun embedAccessorFunction(
        symbol: FirPropertySymbol, functionType: FunctionTypeEmbedding, defaultBehaviour: Boolean
    ): NonInlineFunctionSignature = with(this) {
        val name = if (functionType.paramTypes.isEmpty()) symbol.embedGetterName(this) else symbol.embedSetterName(this)
        functionType.toGenericAccessorSignature(defaultBehaviour).toNamedSignature(name)
            .toNonInlineSignature(symbol = null).toCompleteSignature(symbol.source) {
                preconditions {
                    args {
                        provenInvariants()
                    }
                }
                postconditions {
                    returns {
                        provenInvariants()
                    }
                }
            }
    }.also {
        fullSignatures.putIfAbsent(it.signature.name, it.signature)
    }.signature

    private fun embedExtensionProperty(symbol: FirPropertySymbol): PropertyEmbedding {
        val getterType = buildFunctionPretype {
            withExtensionReceiver { nullableAny() }
            withReturnType { nullableAny() }
        }

        val setterType = buildFunctionPretype {
            withExtensionReceiver { nullableAny() }
            withParam { nullableAny() }
            withReturnType { nullableAny() }
        }

        val getter = embedAccessorFunction(symbol, getterType, defaultBehaviour = false)
        val setter = symbol.isVar.ifTrue { embedAccessorFunction(symbol, setterType, defaultBehaviour = false) }

        return PropertyEmbedding(
            CustomGetter(getter),
            setter?.let { CustomSetter(it) },
            hasDefaultBehaviour = false,
            isUnique = false,
            isVal = symbol.isVal,
            type = embedType(symbol.resolvedReturnType)
        )
    }

    // endregion

    // region types (also classes)

    override fun embedType(type: ConeKotlinType): TypeEmbedding = typeEmbedder.embedType(type)

    override fun embedFunctionPretype(symbol: FirFunctionSymbol<*>): FunctionTypeEmbedding =
        typeEmbedder.embedFunctionPretype(symbol)

    // endregion
}

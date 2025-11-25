/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.ClassPropertyAccess
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyAccessEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.asPropertyAccess
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.isCustom
import org.jetbrains.kotlin.formver.core.isInvariantBuilderFunctionNamed
import org.jetbrains.kotlin.formver.core.linearization.Linearizer
import org.jetbrains.kotlin.formver.core.linearization.PureLinearizer
import org.jetbrains.kotlin.formver.core.linearization.SeqnBuilder
import org.jetbrains.kotlin.formver.core.linearization.SharedLinearizationState
import org.jetbrains.kotlin.formver.core.purity.checkValidity
import org.jetbrains.kotlin.formver.core.purity.isPure
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

/**
 * Interface for statement conversion.
 *
 * Naming convention:
 * - Functions that return a new `StmtConversionContext` should describe what change they make (`addResult`, `removeResult`...)
 * - Functions that take a lambda to execute should describe what extra state the lambda will have (`withResult`...)
 */
interface StmtConversionContext : MethodConversionContext {
    val whenSubject: VariableEmbedding?

    /**
     * In a safe call `callSubject?.foo()` we evaluate the call subject first to check for nullness.
     * In case it is not null, we evaluate the call to `callSubject.foo()`. Here we don't want to evaluate
     * the `callSubject` again to we store it in the `StmtConversionContext`.
     */
    val checkedSafeCallSubject: ExpEmbedding?
    val activeCatchLabels: List<LabelEmbedding>

    fun continueLabelName(targetName: String? = null): SymbolicName
    fun breakLabelName(targetName: String? = null): SymbolicName
    fun addLoopName(targetName: String)
    fun convert(stmt: FirStatement): ExpEmbedding

    fun <R> withNewScope(action: StmtConversionContext.() -> R): R
    fun <R> withNoScope(action: StmtConversionContext.() -> R): R
    fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R

    fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R
    fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R>
}

fun StmtConversionContext.declareLocalProperty(symbol: FirPropertySymbol, initializer: ExpEmbedding?): Declare {
    registerLocalProperty(symbol)
    return Declare(embedLocalProperty(symbol), initializer)
}

fun StmtConversionContext.declareLocalVariable(symbol: FirVariableSymbol<*>, initializer: ExpEmbedding?): Declare {
    registerLocalVariable(symbol)
    return Declare(embedLocalVariable(symbol), initializer)
}

fun StmtConversionContext.declareAnonVar(type: TypeEmbedding, initializer: ExpEmbedding?): Declare =
    Declare(freshAnonVar(type), initializer)


val FirIntersectionOverridePropertySymbol.propertyIntersections
    get() = intersections.filterIsInstanceAnd<FirPropertySymbol> { it.isVal == isVal }

/**
 * Tries to find final property symbol actually declared in some class instead of
 * (potentially) fake property symbol.
 * Note that if some property is found it is fixed since
 * 1. there can't be two non-abstract properties which don't subsume each other
 * in the hierarchy (kotlin disallows that) and final properties can't be abstract;
 * 2. final property can't subsume other final property as that means final property
 * is overridden.
 * //TODO: decide if we leave this lookup or consider it unsafe.
 */
fun FirPropertySymbol.findFinalParentProperty(): FirPropertySymbol? =
    if (this !is FirIntersectionOverridePropertySymbol)
        (isFinal && !isCustom).ifTrue { this }
    else propertyIntersections.firstNotNullOfOrNull { it.findFinalParentProperty() }


/**
 * This is a key function when looking up properties.
 * It translates a kotlin `receiver.field` expression to an `ExpEmbedding`.
 *
 * Note that in FIR this `field` may be represented as `FirIntersectionOverridePropertySymbol`
 * which is necessary when the property could hypothetically inherit from multiple sources.
 * However, we don't register such symbols in the context when traversing the class.
 * Hence, some advanced logic is needed here.
 *
 * First, we try to find an actual backing field somewhere in the parents of the field with a
 * dfs-like algorithm on `FirIntersectionOverridePropertySymbol`s (it also should be final).
 *
 * If final backing field is not found, we lazily create a getter/setter pair for this
 * `FirIntersectionOverrideProperty`.
 */
fun StmtConversionContext.embedPropertyAccess(accessExpression: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = accessExpression.calleeReference.symbol) {
        is FirValueParameterSymbol -> embedParameter(calleeSymbol).asPropertyAccess()
        is FirPropertySymbol -> {
            val type = embedType(calleeSymbol.resolvedReturnType)
            when {
                accessExpression.dispatchReceiver != null -> {
                    val property = calleeSymbol.findFinalParentProperty()?.let {
                        embedProperty(it)
                    } ?: embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.dispatchReceiver!!), property, type)
                }

                accessExpression.extensionReceiver != null -> {
                    val property = embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.extensionReceiver!!), property, type)
                }

                else -> embedLocalProperty(calleeSymbol)
            }
        }

        else ->
            error("Property access symbol $calleeSymbol has unsupported type.")
    }


fun StmtConversionContext.argumentDeclaration(
    arg: ExpEmbedding,
    callType: TypeEmbedding
): Pair<Declare?, ExpEmbedding> =
    when (arg.ignoringMetaNodes()) {
        is LambdaExp -> null to arg
        else -> {
            val argWithInvariants = arg.withNewTypeInvariants(callType) {
                proven = true
                access = true
            }
            // If `argWithInvariants` is `Cast(...(Cast(someVariable))...)` it is fine to use it
            // since in Viper it will always be translated to `someVariable`.
            // On other hand, `TypeEmbedding` and invariants in Viper are guaranteed
            // via previous line.
            if (argWithInvariants.underlyingVariable != null) null to argWithInvariants
            else declareAnonVar(callType, argWithInvariants).let {
                it to it.variable
            }
        }
    }

fun StmtConversionContext.getInlineFunctionCallArgs(
    args: List<ExpEmbedding>,
    formalArgTypes: List<TypeEmbedding>,
): Pair<List<Declare>, List<ExpEmbedding>> {
    val declarations = mutableListOf<Declare>()
    val storedArgs = args.zip(formalArgTypes).map { (arg, callType) ->
        argumentDeclaration(arg, callType).let { (declaration, usage) ->
            declarations.addIfNotNull(declaration)
            usage
        }
    }
    return Pair(declarations, storedArgs)
}

fun StmtConversionContext.insertInlineFunctionCall(
    calleeSignature: FunctionSignature,
    paramNames: List<SubstitutedArgument>,
    args: List<ExpEmbedding>,
    body: FirBlock,
    returnTargetName: String?,
    parentCtx: MethodConversionContext? = null,
): ExpEmbedding {
    // TODO: It seems like it may be possible to avoid creating a local here, but it is not clear how.
    val returnTarget = returnTargetProducer.getFresh(calleeSignature.callableType.returnType)
    val (declarations, callArgs) = getInlineFunctionCallArgs(args, calleeSignature.callableType.formalArgTypes)
    val subs = paramNames.zip(callArgs).toMap()
    val methodCtxFactory = MethodContextFactory(
        calleeSignature,
        InlineParameterResolver(subs, returnTargetName, returnTarget),
        parent = parentCtx,
    )
    return withMethodCtx(methodCtxFactory) {
        Block {
            add(Declare(returnTarget.variable, null))
            addAll(declarations)
            add(FunctionExp(null, convert(body), returnTarget.label))
            // if unit is what we return we might not guarantee it yet
            add(returnTarget.variable.withIsUnitInvariantIfUnit())
        }
    }
}

/**
 * Insert `ForAllEmbedding` where `forAll` function call was encountered.
 */
fun StmtConversionContext.insertForAllFunctionCall(
    symbol: FirValueParameterSymbol,
    block: FirBlock,
): ExpEmbedding {
    val anonVar = freshAnonBuiltinVar(embedType(symbol.resolvedReturnType))
    val methodCtxFactory = MethodContextFactory(
        signature,
        InlineParameterResolver(
            substitutions = mapOf(SubstitutedArgument.ValueParameter(symbol) to anonVar),
            labelName = null,
            // TODO: ideally, there shouldn't be a return target since return is prohibited
            defaultResolvedReturnTarget = defaultResolvedReturnTarget,
        ),
        parent = this,
    )
    return withNoScope {
        withMethodCtx(methodCtxFactory) {
            val (invariants, triggers) = collectInvariantsAndTriggers(block)
            ForAllEmbedding(anonVar, invariants, triggers)
        }
    }
}

fun StmtConversionContext.convertMethodWithBody(
    declaration: FirSimpleFunction,
    signature: FullNamedFunctionSignature,
    returnTarget: ReturnTarget,
): FunctionBodyEmbedding? {
    val firBody = declaration.body ?: return null
    val body = convert(firBody)
    val bodyExp = FunctionExp(signature, body, returnTarget.label)
    val seqnBuilder = SeqnBuilder(declaration.source)
    val linearizer = Linearizer(SharedLinearizationState(anonVarProducer), seqnBuilder, declaration.source)
    bodyExp.toViperUnusedResult(linearizer)
    // note: we must guarantee somewhere that returned value is Unit
    // as we may not encounter any `return` statement in the body
    returnTarget.variable.withIsUnitInvariantIfUnit().toViperUnusedResult(linearizer)

    // TODO: Stop translation if this check fails
    body.checkValidity(declaration.source, errorCollector)

    return FunctionBodyEmbedding(seqnBuilder.block, returnTarget, bodyExp)
}

fun StmtConversionContext.convertFunctionWithBody(
    declaration: FirSimpleFunction
): Exp {
    val firBody = declaration.body ?: throw SnaktInternalException(
        declaration.source,
        "Pure functions expect a function body to exist"
    )
    val bodyWithPosition = convert(firBody)
    // TODO: Clean this up
    val body = extractReturnedExprFromPureFunctionBody(bodyWithPosition, declaration.source)
    val pureLinearizer = PureLinearizer(declaration.source)

    if (!body.isPure()) throw SnaktInternalException(
        declaration.source,
        "Impure function body detected in pure function"
    )
    return body.returnExp.toViper(pureLinearizer)
}

private fun extractReturnedExprFromPureFunctionBody(body: ExpEmbedding, source: KtSourceElement?): Return =
    when (body) {
        is WithPosition -> extractReturnedExprFromPureFunctionBody(body.inner, source)
        is Block -> {
            val relevantBody = body.ignoringMetaNodes()
            if (relevantBody.exps.size != 1) throw SnaktInternalException(
                source,
                "The body of a pure function may only contain a block with one expression! Got body $body"
            )
            extractReturnedExprFromPureFunctionBody(relevantBody.exps.single(), source)
        }

        is Return -> body
        else -> throw SnaktInternalException(
            source,
            "Pure functions currently only support literal returns! Got body $body"
        )
    }

private const val INVALID_STATEMENT_MSG =
    "Every statement in invariant block must be a pure boolean invariant."

data class InvariantsAndTriggers(
    val invariants: List<ExpEmbedding>,
    val triggers: List<ExpEmbedding>
)

fun StmtConversionContext.collectInvariants(block: FirBlock) = buildList {
    block.statements.forEach { stmt ->
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        add(stmt.accept(StmtConversionVisitor, this@collectInvariants))
    }
}

/**
 * Attempts to extract trigger expressions from a triggers() function call.
 * Returns the list of trigger expressions if this is a triggers() call, or null otherwise.
 */
private fun StmtConversionContext.tryExtractTriggers(stmt: FirStatement): List<ExpEmbedding>? {
    if (stmt !is FirFunctionCall) return null

    val symbol = stmt.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
    if (symbol?.isInvariantBuilderFunctionNamed("triggers") != true) return null

    val varargs = stmt.arguments.firstOrNull() as? FirVarargArgumentsExpression
        ?: throw IllegalArgumentException("triggers() function must have a single varargs parameter.")

    // TODO: check whether trigger is valid in Viper.
    return varargs.arguments.map { expr ->
        expr.accept(StmtConversionVisitor, this)
    }
}

fun StmtConversionContext.collectInvariantsAndTriggers(block: FirBlock): InvariantsAndTriggers {
    val invariants = mutableListOf<ExpEmbedding>()
    val triggers = mutableListOf<ExpEmbedding>()

    block.statements.forEach { stmt ->
        val extractedTriggers = tryExtractTriggers(stmt)
        if (extractedTriggers != null) {
            triggers.addAll(extractedTriggers)
            return@forEach
        }

        // Otherwise, treat as invariant
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        invariants.add(stmt.accept(StmtConversionVisitor, this))
    }

    return InvariantsAndTriggers(invariants, triggers)
}


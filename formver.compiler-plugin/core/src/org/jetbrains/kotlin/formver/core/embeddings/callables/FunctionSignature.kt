/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.conversion.SubstitutedArgument
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.conversion.insertInlineFunctionCall
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.MethodCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*

interface FunctionSignature {
    val callableType: FunctionTypeEmbedding
    val dispatchReceiver: VariableEmbedding?
    val extensionReceiver: VariableEmbedding?

    val params: List<VariableEmbedding>

    val returns: VariableEmbedding

    val isPure: Boolean

    val labelName: String?
        get() = null

    val formalArgs: List<VariableEmbedding>
        get() = listOfNotNull(dispatchReceiver, extensionReceiver) + params
}

data class FunctionSignatureImpl(
    override val callableType: FunctionTypeEmbedding,
    override val dispatchReceiver: VariableEmbedding?,
    override val extensionReceiver: VariableEmbedding?,
    override val params: List<VariableEmbedding>,
    override val returns: VariableEmbedding,
    override val isPure: Boolean,
    override val labelName: String? = null,
) : FunctionSignature


interface NamedFunctionSignature : FunctionSignature {
    val name: SymbolicName

    val symbol: FirFunctionSymbol<*>?

    override val labelName: String?
        get() = symbol?.name?.asString()
}

data class NamedFunctionSignatureImpl(
    val signature: FunctionSignature, override val name: SymbolicName, override val symbol: FirFunctionSymbol<*>?
) : NamedFunctionSignature, FunctionSignature by signature {
    override val labelName: String?
        get() = super<NamedFunctionSignature>.labelName
}


interface NamedFunctionSignatureWithContract : NamedFunctionSignature {
    /**
     * Preconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val preconditions: List<ExpEmbedding>

    /**
     * Postconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val postconditions: List<ExpEmbedding>

    val declarationSource: KtSourceElement?
}

interface NamedCallableEmbedding : NamedFunctionSignature, CallableEmbedding


interface NonInlineCallable : NamedCallableEmbedding {

    override fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding = insertCall(args)

    // When the function does not need to be inlined, we do not need a context to call it. This is helpful, because
    // in some situations, we do not want to pass around the context (or we don't have access to it)
    fun insertCall(args: List<ExpEmbedding>): ExpEmbedding = if (isPure) {
        FunctionCall(this, args)
    } else {
        MethodCall(this, args)
    }
}

data class NonInlineCallableImpl(
    val functionSignature: NamedFunctionSignature, override val symbol: FirFunctionSymbol<*>?
) : NonInlineCallable, NamedFunctionSignature by functionSignature {
    override val labelName: String? = symbol?.name?.asString()
}

fun NonInlineCallable.toMethodCall(
    parameters: List<Exp>,
    target: Exp.LocalVar,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) = Stmt.MethodCall(name, parameters, listOf(target), pos, info)

fun NonInlineCallable.toFuncApp(
    parameters: List<Exp>,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) = Exp.FuncApp(name, parameters, Type.Ref, pos, info)


data class NonInlineFunctionSignature(
    val signature: NamedFunctionSignature,
    override val preconditions: List<ExpEmbedding>,
    override val postconditions: List<ExpEmbedding>,
    override val declarationSource: KtSourceElement?
) : CompleteFunctionSignature, NonInlineCallable, NamedFunctionSignature by signature


interface CompleteFunctionSignature : NamedFunctionSignatureWithContract, NamedCallableEmbedding

fun CompleteFunctionSignature.toViperMethod(ctx: TypeResolver, body: Stmt.Seqn?): Method {
    require(!isPure) {
        "Pure functions should not be converted to methods"
    }
    return UserMethod(
        name,
        formalArgs.map { it.toLocalVarDecl() },
        returns.toLocalVarDecl(),
        preconditions.pureToViper(toBuiltin = true, ctx),
        postconditions.pureToViper(toBuiltin = true, ctx),
        body,
        declarationSource.asPosition
    )
}

fun CompleteFunctionSignature.toViperFunction(
    ctx: TypeResolver,
    body: Exp?,
): UserFunction {
    require(isPure) {
        "Impure functions should not be converted to functions"
    }
    return UserFunction(
        name,
        formalArgs.map { it.toLocalVarDecl() },
        // TODO: Be explicit about the return types of functions instead of boxing them into a Ref
        Type.Ref,
        preconditions.pureToViper(toBuiltin = true, ctx),
        postconditions.pureToViper(toBuiltin = true, ctx) + terminationMeasure(),
        body,
        declarationSource.asPosition
    )
}

/**
 * The measure this function must decrease at every recursive call, or nothing if none can be built.
 *
 * A `@Unique` parameter owns the structure reachable from it, and a recursive `@Pure` function over
 * such a parameter descends into that ownership, so the parameter's unique predicate instance orders
 * the calls: nesting of predicate instances is the one ordering on the heap Viper knows about.
 *
 * Each instance is preceded by a nullity test because the unique predicate nests the next link only
 * where that link is non-null. At the call that reaches the end of the structure the two instances
 * are unrelated and the nullity component is what decreases; elsewhere it is constant and the
 * instance decides. Parameters contribute in declaration order, compared lexicographically.
 *
 * A function with no `@Unique` parameter gets no measure, and so cannot be called from one that has
 * a measure: there is nothing to build a measure from in that case.
 */
private fun CompleteFunctionSignature.terminationMeasure(): List<Exp> {
    val components = formalArgs.filter { it.isUnique }.flatMap { arg ->
        val classType = arg.type.pretype as? ClassTypeEmbedding ?: return@flatMap emptyList()
        listOf(
            Exp.NeCmp(arg.toLocalVarUse(), RuntimeTypeDomain.nullValue()),
            PredicateInstance(classType.uniquePredicateName, listOf(arg.toLocalVarUse())),
        )
    }
    return if (components.isEmpty()) emptyList() else listOf(DecreasesTuple(components))
}


data class InlineNamedFunction(
    val signature: NamedFunctionSignature,
    val firBody: FirBlock,
    override val preconditions: List<ExpEmbedding>,
    override val postconditions: List<ExpEmbedding>,
    override val symbol: FirFunctionSymbol<*>,
) : CompleteFunctionSignature, NamedCallableEmbedding, NamedFunctionSignature by signature {
    override fun insertCall(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding {
        val paramNames = buildList {
            if (callableType.dispatchReceiverType != null) add(SubstitutedArgument.DispatchThis)
            if (callableType.extensionReceiverType != null) add(SubstitutedArgument.ExtensionThis)
            addAll(symbol.valueParameterSymbols.map { SubstitutedArgument.ValueParameter(it) })
        }
        return ctx.insertInlineFunctionCall(signature, paramNames, args, firBody, signature.labelName)
    }

    override val declarationSource: KtSourceElement?
        get() = symbol.source
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.underlyingVariable
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Name resolver for parameters and return values and labels.
 *
 * Since parameter names may map to lambda embeddings, we use `embed` for those for consistency.
 */
interface ParameterResolver {
    fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding?
    fun tryResolveDispatchReceiver(): ExpEmbedding?
    fun tryResolveExtensionReceiver(labelName: String): ExpEmbedding?

    fun retrieveAllParams(): Sequence<VariableEmbedding>

    val labelName: String?
    val defaultResolvedReturnTarget: ReturnTarget
}

fun ParameterResolver.resolveNamedReturnTarget(returnPointName: String): ReturnTarget? =
    (returnPointName == labelName).ifTrue { defaultResolvedReturnTarget }

class RootParameterResolver(
    val ctx: ProgramConversionContext,
    private val signature: FunctionSignature,
    firArgs: List<FirValueParameterSymbol>,
    override val labelName: String?,
    override val defaultResolvedReturnTarget: ReturnTarget,
) : ParameterResolver {
    private val parameters = firArgs.zip(signature.params).toMap()
    override fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding? = parameters[symbol]
    override fun tryResolveDispatchReceiver() = signature.dispatchReceiver
    override fun tryResolveExtensionReceiver(labelName: String) = (labelName == this.labelName).ifTrue {
        signature.extensionReceiver
    }

    override fun retrieveAllParams(): Sequence<VariableEmbedding> = sequence {
        yieldAll(signature.params)
    }
}

interface ReturnVarSubstitutionContext {
    fun resolve(symbol: FirValueParameterSymbol): VariableEmbedding?
}

class ReturnVarSubstitutor(val substitutionSymbol: FirValueParameterSymbol, val variable: VariableEmbedding) : ReturnVarSubstitutionContext {
    override fun resolve(symbol: FirValueParameterSymbol) = (symbol == substitutionSymbol).ifTrue { variable }
}

class SubstitutedReturnParameterResolver(
    private val rootResolver: RootParameterResolver,
    private val substitutionContext: ReturnVarSubstitutionContext,
) : ParameterResolver by rootResolver {
    override fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding? =
        substitutionContext.resolve(symbol) ?: rootResolver.tryResolveParameter(symbol)
}

/**
 * Wrapper class: in inline functions we want to substitute actual parameters with our own anonymous variables with unique names.
 */
sealed interface SubstitutedArgument {
    data class ValueParameter(val symbol: FirValueParameterSymbol) : SubstitutedArgument
    data object ExtensionThis : SubstitutedArgument
    data object DispatchThis : SubstitutedArgument
}

class InlineParameterResolver(
    private val substitutions: Map<SubstitutedArgument, ExpEmbedding>,
    override val labelName: String?,
    override val defaultResolvedReturnTarget: ReturnTarget,
) : ParameterResolver {
    override fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding? =
        substitutions[SubstitutedArgument.ValueParameter(symbol)]

    override fun tryResolveDispatchReceiver() = substitutions[SubstitutedArgument.DispatchThis]
    override fun tryResolveExtensionReceiver(labelName: String) = (labelName == this.labelName).ifTrue {
        substitutions[SubstitutedArgument.ExtensionThis]
    }

    override fun retrieveAllParams(): Sequence<VariableEmbedding> = sequence {
        yieldAll(substitutions.values.asSequence().map { it.underlyingVariable!! })
    }
}
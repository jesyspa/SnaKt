/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding

/**
 * The symbol resolution data for a single method.
 *
 * Method converters are chained syntactically; the converter of a lambda has the method that the lambda is defined in as a parent.
 * In general, however, a callee inline function does *not* in general have its caller as a parent: this is because an inlined
 * function does not have access to the variables of its caller, so it does not make sense to have symbol resolution pass through it.
 *
 * We're using the term `MethodConverter` here for consistency with the `XConverter` implementing `XConversionContext`.
 * Really, this class doesn't do any conversion itself, it just provides information for the `StmtConverter`
 * to get its work done.
 */
class MethodConverter(
    private val programCtx: ProgramConversionContext,
    override val signature: FunctionSignature,
    private val paramResolver: ParameterResolver,
    scopeDepth: ScopeIndex,
    private val parent: MethodConversionContext? = null,
) : MethodConversionContext, ProgramConversionContext by programCtx {
    private var propertyResolver = PropertyResolver(scopeDepth)

    override val isValidForForAllBlock: Boolean
        get() = !propertyResolver.canCreateLocals

    override fun <R> withScopeImpl(scopeDepth: ScopeIndex, action: () -> R): R {
        propertyResolver = propertyResolver.innerScope(scopeDepth)
        val result = action()
        propertyResolver = propertyResolver.parent!!
        return result
    }

    override fun addLoopIdentifier(labelName: String, index: Int) {
        propertyResolver = propertyResolver.addLoopIdentifier(labelName, index)
    }

    override fun resolveLoopIndex(name: String): Int =
        propertyResolver.tryResolveLoopName(name) ?: throw IllegalArgumentException("Loop $name not found in scope.")

    override fun resolveLocal(symbol: FirVariableSymbol<*>): VariableEmbedding =
        propertyResolver.tryResolveLocalProperty(symbol) ?: parent?.resolveLocal(symbol)
        ?: throw IllegalArgumentException("Property ${symbol.name} not found in scope.")

    override fun registerLocalProperty(symbol: FirPropertySymbol) {
        if (symbol.name.isSpecial)
            propertyResolver.registerSpecialProperty(symbol, freshAnonVar(embedType(symbol.resolvedReturnType)))
        else
            propertyResolver.registerLocalProperty(symbol, embedType(symbol.resolvedReturnType))
    }

    override fun registerLocalVariable(symbol: FirVariableSymbol<*>) {
        propertyResolver.registerLocalVariable(symbol, embedType(symbol.resolvedReturnType))
    }

    override fun resolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding =
        paramResolver.tryResolveParameter(symbol) ?: parent?.resolveParameter(symbol)
        ?: throw IllegalArgumentException("Parameter ${symbol.name} not found in scope.")

    override fun resolveDispatchReceiver(): ExpEmbedding? =
        paramResolver.tryResolveDispatchReceiver() ?: parent?.resolveDispatchReceiver()

    override fun resolveExtensionReceiver(labelName: String): ExpEmbedding? =
        paramResolver.tryResolveExtensionReceiver(labelName) ?: parent?.resolveExtensionReceiver(labelName)

    override val defaultResolvedReturnTarget = paramResolver.defaultResolvedReturnTarget
    override fun resolveNamedReturnTarget(labelName: String): ReturnTarget? =
        paramResolver.resolveNamedReturnTarget(labelName) ?: parent?.resolveNamedReturnTarget(labelName)

    override fun retrievePropertiesAndParameters(): Sequence<VariableEmbedding> = sequence {
        yieldAll(propertyResolver.retrieveAllProperties())
        yieldAll(paramResolver.retrieveAllParams())
        parent?.retrievePropertiesAndParameters()?.let { yieldAll(it) }
    }
}
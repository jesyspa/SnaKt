/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.names.ReturnLabelName
import org.jetbrains.kotlin.formver.core.names.ReturnVariableName

class ReturnTarget(depth: Int, type: TypeEmbedding) {
    val variable = PlaceholderVariableEmbedding(ReturnVariableName(depth), type)
    val label = LabelEmbedding(ReturnLabelName(depth))
}

/**
 * Context for converting a method body.
 *
 * We use the terms `register`, `resolve`, and `embed` a lot here. For consistency:
 * - `register` takes a name or symbol and an embedding and stores it.
 * - `resolve` takes a name and retrieves an already-existing embedding.
 * - `embed` takes a symbol and returns an embedding; this embedding may be existing or new.
 */
interface MethodConversionContext : ProgramConversionContext {
    val signature: FunctionSignature
    val defaultResolvedReturnTarget: ReturnTarget
    val isValidForForAllBlock: Boolean

    fun resolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding
    fun resolveLocal(symbol: FirVariableSymbol<*>): VariableEmbedding
    fun registerLocalProperty(symbol: FirPropertySymbol)
    fun registerLocalVariable(symbol: FirVariableSymbol<*>)
    fun resolveDispatchReceiver(): ExpEmbedding?
    fun resolveExtensionReceiver(labelName: String): ExpEmbedding?

    fun <R> withScopeImpl(scopeDepth: ScopeIndex, action: () -> R): R
    fun addLoopIdentifier(labelName: String, index: Int)
    fun resolveLoopIndex(name: String): Int
    fun resolveNamedReturnTarget(labelName: String): ReturnTarget?
    fun retrievePropertiesAndParameters(): Sequence<VariableEmbedding>
}

fun MethodConversionContext.resolveReturnTarget(targetSourceName: String?): ReturnTarget =
    if (targetSourceName == null) defaultResolvedReturnTarget
    else resolveNamedReturnTarget(targetSourceName)
        ?: throw IllegalArgumentException("Cannot resolve returnTarget of $targetSourceName")

fun MethodConversionContext.embedLocalProperty(symbol: FirPropertySymbol): VariableEmbedding = resolveLocal(symbol)
fun MethodConversionContext.embedParameter(symbol: FirValueParameterSymbol): ExpEmbedding = resolveParameter(symbol)
fun MethodConversionContext.embedLocalVariable(symbol: FirVariableSymbol<*>): VariableEmbedding = resolveLocal(symbol)

fun MethodConversionContext.embedLocalSymbol(symbol: FirBasedSymbol<*>): ExpEmbedding =
    when (symbol) {
        is FirValueParameterSymbol -> embedParameter(symbol)
        is FirPropertySymbol -> embedLocalProperty(symbol)
        is FirVariableSymbol<*> -> embedLocalVariable(symbol)
        else -> throw IllegalArgumentException("Symbol $symbol cannot be embedded as a local symbol.")
    }

fun MethodConversionContext.statementCtxt(): StmtConversionContext = StmtConverter(this)
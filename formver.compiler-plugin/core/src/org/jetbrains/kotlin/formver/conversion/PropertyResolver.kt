/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FirVariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.names.embedScopedLocalName
import org.jetbrains.kotlin.name.Name

data class LoopIdentifier(val targetName: String, val index: Int)

/**
 * Resolver for names of local properties.
 *
 * This is a stacked resolver: the resolver for the innermost scope contains a reference
 * to the resolver for the outer scopes, and automatically searches them.
 */
class PropertyResolver(
    private val scopeIndex: ScopeIndex,
    val parent: PropertyResolver? = null,
    private val loopName: LoopIdentifier? = null,
) {
    private val variables: MutableMap<FirVariableSymbol<*>, VariableEmbedding> = mutableMapOf()

    val canCreateLocals: Boolean
        get() = scopeIndex is ScopeIndex.Indexed

    fun tryResolveLocalProperty(symbol: FirVariableSymbol<*>): VariableEmbedding? =
        variables[symbol] ?: parent?.tryResolveLocalProperty(symbol)

    fun registerLocalProperty(symbol: FirPropertySymbol, type: TypeEmbedding) {
        check(symbol.isLocal) { "PropertyResolver must be used with local properties." }
        registerLocal(symbol.name, type, symbol)
    }

    fun registerSpecialProperty(symbol: FirPropertySymbol, anonVar: VariableEmbedding) {
        variables[symbol] = anonVar
    }

    fun registerLocalVariable(symbol: FirVariableSymbol<*>, type: TypeEmbedding) {
        registerLocal(symbol.name, type, symbol)
    }

    private fun registerLocal(name: Name, type: TypeEmbedding, symbol: FirVariableSymbol<*>) {
        variables[symbol] = FirVariableEmbedding(name.embedScopedLocalName(scopeIndex), type, symbol)
    }

    fun innerScope(innerScopeIndex: ScopeIndex) = PropertyResolver(innerScopeIndex, this)

    fun addLoopIdentifier(labelName: String, index: Int) = PropertyResolver(scopeIndex, parent, LoopIdentifier(labelName, index))

    fun tryResolveLoopName(name: String): Int? =
        if (loopName?.targetName == name) loopName.index
        else parent?.tryResolveLoopName(name)

    fun retrieveAllProperties(): Sequence<VariableEmbedding> = sequence {
        yieldAll(variables.values)
        parent?.retrieveAllProperties()?.let { yieldAll(it) }
    }
}
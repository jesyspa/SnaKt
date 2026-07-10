/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LocalFunctionDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.utils.addIfNotNull

class GraphDeclaredSymbolsResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory =
            Factory { currentSession -> GraphDeclaredSymbolsResolver(currentSession) }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, _: Unit ->
        extractDeclaredSymbolsOf(graph)
    }

    fun resolveDeclaredSymbolsOf(graph: ControlFlowGraph): Set<FirBasedSymbol<*>> =
        cache.getValue(graph, Unit)

    private fun extractDeclaredSymbolsOf(graph: ControlFlowGraph): Set<FirBasedSymbol<*>> {
        val declarations = mutableSetOf<FirBasedSymbol<*>>()

        when (val declaration = graph.declaration) {
            is FirFunction -> {
                declarations.add(declaration.symbol)
                declarations.addIfNotNull(declaration.receiverParameter?.symbol)
                declaration.contextParameters.mapTo(declarations) { it.symbol }
                declaration.valueParameters.mapTo(declarations) { it.symbol }
            }

            else -> { }
        }

        graph.nodes
            .mapNotNull { node ->
                (node as? VariableDeclarationNode)?.fir?.symbol ?:
                (node as? LocalFunctionDeclarationNode)?.fir?.symbol
            }
            .forEach(declarations::add)

        return declarations
    }
}

private val FirSession.graphDeclaredSymbolsResolver: GraphDeclaredSymbolsResolver
    by FirSession.sessionComponentAccessor()

/**
 * Resolves symbols declared directly by [this] graph.
 *
 * For function graphs this includes receiver, context, and value parameters, and for all graphs it includes symbols
 * introduced by [VariableDeclarationNode] entries in the graph nodes.
 *
 * @param context Is used to access this session's [GraphDeclaredSymbolsResolver].
 *
 * The result is memoized by a session cache.
 */
context(context: CheckerContext)
fun ControlFlowGraph.resolveDeclaredSymbols(): Set<FirBasedSymbol<*>> =
    context.session.graphDeclaredSymbolsResolver.resolveDeclaredSymbolsOf(this)

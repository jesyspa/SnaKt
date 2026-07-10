/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CallableReferenceNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.utils.addIfNotNull

class GraphUsedSymbolsResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory =
            Factory { currentSession -> GraphUsedSymbolsResolver(currentSession) }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, _: Unit ->
        extractUsedSymbolsOf(graph)
    }

    fun resolveUsedSymbolsOf(graph: ControlFlowGraph): Set<FirBasedSymbol<*>> =
        cache.getValue(graph, Unit)

    private fun extractUsedSymbolsOf(graph: ControlFlowGraph): Set<FirBasedSymbol<*>> {
        val usages = mutableSetOf<FirBasedSymbol<*>>()

        graph.nodes.forEach { node ->
            when (node) {
                is QualifiedAccessNode ->
                    usages.addIfNotNull(node.fir.calleeReference.symbol)

                is CallableReferenceNode ->
                    usages.addIfNotNull(node.fir.calleeReference.symbol)

                else -> { }
            }
        }

        graph.subGraphs.flatMapTo(usages, ::extractUsedSymbolsOf)

        return usages
    }
}

private val FirSession.graphUsedSymbolsResolver: GraphUsedSymbolsResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveUsedSymbols(): Set<FirBasedSymbol<*>> =
    context.session.graphUsedSymbolsResolver.resolveUsedSymbolsOf(this)

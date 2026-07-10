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

class GraphCapturedSymbolsResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory =
            Factory { currentSession -> GraphCapturedSymbolsResolver(currentSession) }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        with(context) {
            extractCapturedSymbolsOf(graph)
        }
    }

    fun resolveCapturedSymbolsOf(graph: ControlFlowGraph, context: CheckerContext): Set<FirBasedSymbol<*>> =
        cache.getValue(graph, context)

    context(context: CheckerContext)
    private fun extractCapturedSymbolsOf(graph: ControlFlowGraph): Set<FirBasedSymbol<*>> {
        val capturedSymbols = mutableSetOf<FirBasedSymbol<*>>()

        for (node in graph.nodes) {
            when (node) {
                is QualifiedAccessNode ->
                    capturedSymbols.addIfNotNull(node.fir.calleeReference.symbol)

                is CallableReferenceNode ->
                    capturedSymbols.addIfNotNull(node.fir.calleeReference.symbol)

                else -> { }
            }
        }

        graph.subGraphs.flatMapTo(capturedSymbols, { it.resolveCapturedSymbols() })
        val declaredSymbols = graph.resolveDeclaredSymbols()

        return capturedSymbols - declaredSymbols
    }
}

private val FirSession.graphCapturedSymbolsResolver: GraphCapturedSymbolsResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveCapturedSymbols(): Set<FirBasedSymbol<*>> =
    context.session.graphCapturedSymbolsResolver.resolveCapturedSymbolsOf(this, context)

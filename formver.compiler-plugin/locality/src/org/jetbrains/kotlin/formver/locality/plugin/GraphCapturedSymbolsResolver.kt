/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

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
        val declaredSymbols = graph.resolveDeclaredSymbols()
        val usedSymbols = graph.resolveUsedSymbols()

        return usedSymbols - declaredSymbols
    }
}

private val FirSession.graphCapturedSymbolsResolver: GraphCapturedSymbolsResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveCapturedSymbols(): Set<FirBasedSymbol<*>> =
    context.session.graphCapturedSymbolsResolver.resolveCapturedSymbolsOf(this, context)

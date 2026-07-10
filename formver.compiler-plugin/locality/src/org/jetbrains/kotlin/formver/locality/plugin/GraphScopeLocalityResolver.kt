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
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol

class GraphScopeLocalityResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory =
            Factory { currentSession -> GraphScopeLocalityResolver(currentSession) }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        with(context) {
            extractScopeLocalityOf(graph)
        }
    }

    fun resolveScopeLocalityOf(graph: ControlFlowGraph, context: CheckerContext): Locality =
        cache.getValue(graph, context)

    context(context: CheckerContext)
    private fun extractScopeLocalityOf(graph: ControlFlowGraph): Locality {
        val capturedSymbols = graph.resolveCapturedSymbols()

        if (capturedSymbols.any { symbol -> extractCapturedSymbolLocality(symbol) == Locality.Local }) {
            return Locality.Local
        }

        return Locality.Global
    }

    context(context: CheckerContext)
    private fun extractCapturedSymbolLocality(symbol: FirBasedSymbol<*>): Locality =
        when (symbol) {
            is FirCallableSymbol<*> -> symbol.resolveLocality()
            is FirReceiverParameterSymbol -> symbol.resolveLocality()
            else -> Locality.Global
        }
}

private val FirSession.graphScopeLocalityResolver: GraphScopeLocalityResolver
    by FirSession.sessionComponentAccessor()

/**
 * Resolves locality of this graph's scope from captured symbols.
 *
 * The scope is [Locality.Local] when at least one captured symbol resolves to local locality; otherwise the scope is
 * [Locality.Global].
 *
 * @param context Is for resolving captured symbols with [resolveCapturedSymbols] and for accessing this session's
 *  [GraphScopeLocalityResolver].
 *
 * The result is memoized by a session cache.
 */
context(context: CheckerContext)
fun ControlFlowGraph.resolveScopeLocality(): Locality =
    context.session.graphScopeLocalityResolver.resolveScopeLocalityOf(this, context)

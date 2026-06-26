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
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

class GraphLocalPropertySymbolsResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphLocalPropertySymbolsResolver(session) }
        }
    }

    private val cacheFactory = session.firCachesFactory

    private val cache =
        cacheFactory.createCache { graph: ControlFlowGraph, _: Unit ->
            extractLocalPropertiesOf(graph)
        }

    fun resolveLocalPropertiesOf(graph: ControlFlowGraph): Set<FirLocalPropertySymbol> =
        cache.getValue(graph, Unit)

    fun extractLocalPropertiesOf(graph: ControlFlowGraph): Set<FirLocalPropertySymbol> {
        return graph.nodes.mapNotNull { node ->
            (node as? VariableDeclarationNode)?.fir?.symbol as? FirLocalPropertySymbol
        }.toSet()
    }
}

private val FirSession.graphLocalPropertySymbolsResolver: GraphLocalPropertySymbolsResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveLocalProperties(): Set<FirLocalPropertySymbol> =
    context.session.graphLocalPropertySymbolsResolver.resolveLocalPropertiesOf(this)

context(context: CheckerContext)
fun FirFunctionSymbol<*>.declares(symbol: FirBasedSymbol<*>): Boolean =
    when (symbol) {
        is FirReceiverParameterSymbol ->
            symbol.containingDeclarationSymbol == this
        is FirValueParameterSymbol ->
            symbol.containingDeclarationSymbol == this
        is FirLocalPropertySymbol -> {
            val graph = resolvedControlFlowGraphReference?.controlFlowGraph
                ?: return false

            symbol in graph.resolveLocalProperties()
        }
        else -> false
    }

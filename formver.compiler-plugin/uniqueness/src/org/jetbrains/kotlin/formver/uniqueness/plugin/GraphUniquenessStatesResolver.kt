/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.formver.locality.plugin.CallArgumentLocalitiesMapper

/**
 * Checks whether a call doesn't modify the uniqueness state (i.e. it is uniqueness-neutral).
 */
fun interface UniquenessNeutralCallPredicate {
    context(context: CheckerContext)
    fun accepts(call: FirCall): Boolean
}

/**
 * Session component that caches uniqueness-state flow analysis for control-flow graphs.
 *
 * @param uniquenessNeutralCallPredicate Is a predicate returning `true` if a call does not change the uniqueness state,
 *  `false` otherwise.
 */
class GraphUniquenessStatesResolver(
    private val uniquenessNeutralCallPredicate: UniquenessNeutralCallPredicate,
    session: FirSession
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(uniquenessNeutralCallPredicate: UniquenessNeutralCallPredicate = { false }): Factory {
            return Factory { session -> GraphUniquenessStatesResolver(uniquenessNeutralCallPredicate, session) }
        }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        analyzeUniquenessStatesOf(graph, context)
    }

    fun resolveUniquenessStateFlowsOf(
        graph: ControlFlowGraph,
        context: CheckerContext
    ): Map<CFGNode<*>, PathAwareUniquenessStateFlow> =
        cache.getValue(graph, context)

    private fun analyzeUniquenessStatesOf(
        graph: ControlFlowGraph,
        context: CheckerContext
    ): Map<CFGNode<*>, PathAwareUniquenessStateFlow> {
        val declaration = graph.declaration
        var initialState = EmptyUniquenessState

        if (declaration is FirFunction) {
            context(context) {
                val receiverParameter = declaration.receiverParameter

                if (receiverParameter != null) {
                    val receiverParameterSymbol = receiverParameter.symbol
                    initialState = initialState.putChild(
                        receiverParameter.symbol,
                        UniquenessState(receiverParameterSymbol.resolveUniqueness())
                    )
                }

                for (valueParameter in declaration.valueParameters) {
                    val valueParameterSymbol = valueParameter.symbol
                    initialState = initialState.putChild(
                        valueParameterSymbol,
                        UniquenessState(valueParameterSymbol.resolveUniqueness())
                    )
                }

            }
        }

        val analyzer = GraphUniquenessStatesAnalyzer(
            initialState,
            context,
            CallArgumentLocalitiesMapper,
            uniquenessNeutralCallPredicate
        )

        return graph.traverseToFixedPoint(analyzer)
    }
}

private val FirSession.graphUniquenessStatesResolver: GraphUniquenessStatesResolver
        by FirSession.sessionComponentAccessor()

/**
 * Resolves the uniqueness-state flow analysis for [this] graph.
 */
context(context: CheckerContext)
fun ControlFlowGraph.resolveUniquenessStateFlows(): Map<CFGNode<*>, PathAwareUniquenessStateFlow> =
    context.session.graphUniquenessStatesResolver.resolveUniquenessStateFlowsOf(this, context)

/**
 * Reads the uniqueness state before [node] by joining the output states of its predecessors.
 */
fun Map<CFGNode<*>, PathAwareUniquenessStateFlow>.readInputUniquenessStateOf(node: CFGNode<*>): UniquenessState? =
    node.previousCfgNodes
        .map { predecessor -> this[predecessor].joinOverEdgeKinds() }
        .reduceOrNull(UniquenessState::join)

/**
 * Reads the uniqueness state after [node] by joining all path edge kinds.
 */
fun Map<CFGNode<*>, PathAwareUniquenessStateFlow>.readOutputUniquenessStateOf(node: CFGNode<*>): UniquenessState =
    this[node].joinOverEdgeKinds()

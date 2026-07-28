/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.merge
import org.jetbrains.kotlin.fir.analysis.cfa.util.transformValues
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.allReceiverExpressions
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterValueParameterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitDefaultArgumentsNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ThrowExceptionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.locality.plugin.resolveLocality
import org.jetbrains.kotlin.formver.type.plugin.CallArgumentTypeFactsMapper

typealias UniquenessStateFlow = ControlFlowInfo<Unit, UniquenessState>

typealias PathAwareUniquenessStateFlow = PathAwareControlFlowInfo<Unit, UniquenessState>

/**
 * Returns the join of the [UniquenessStateFlow]s over every path edge kind.
 */
fun PathAwareUniquenessStateFlow?.joinOverEdgeKinds(): UniquenessState =
    this?.values
        ?.map { it[Unit] ?: EmptyUniquenessState }
        ?.reduceOrNull(UniquenessState::join)
        ?: EmptyUniquenessState

private val CFGNodeWithSubgraphs<*>.extendsLocalFlow: Boolean
    get() = fir is FirValueParameter

/**
 * Returns the nodes of [this] graph that are analyzed by [GraphUniquenessStatesAnalyzer]
 */
val ControlFlowGraph.uniquenessAnalysisTargetNodes: Sequence<CFGNode<*>>
    get() = nodes.asSequence().flatMap { node ->
        if (node is EnterValueParameterNode) {
            node.subGraphs.asSequence().flatMap { subGraph -> subGraph.nodes.asSequence() }
        } else {
            sequenceOf(node)
        }
    }


/**
 * Data-flow analyzer that tracks the uniqueness state of paths through a CFG.
 *
 * Assignments and declarations initialize their target paths and move their source paths. Function calls move all
 * passed paths on entry, and restore paths whose corresponding parameters are local on exit.
 */
class GraphUniquenessStatesAnalyzer(
    private val initialState: UniquenessState,
    private val context: CheckerContext,
    private val callArgumentLocalitiesMapper: CallArgumentTypeFactsMapper<Locality>,
    private val uniquenessNeutralCallPredicate: UniquenessNeutralCallPredicate
) : PathAwareControlFlowGraphVisitor<Unit, UniquenessState>() {
    override fun mergeInfo(
        a: UniquenessStateFlow,
        b: UniquenessStateFlow,
        node: CFGNode<*>
    ): UniquenessStateFlow =
        a.merge(b) { leftState, rightState ->
            leftState.join(rightState)
        }

    private fun UniquenessStateFlow.getOrInitialize(): UniquenessState =
        this[Unit] ?: initialState

    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean {
        return node.extendsLocalFlow
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        return data.transformValues { data -> data.put(Unit, data.getOrInitialize()) }
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val declaration = node.fir
        val initializer = declaration.initializer
        val leftSymbol = declaration.symbol
        val leftAccessState = EmptyAccessState.putChild(
            leftSymbol,
            AccessState(Access.Terminal)
        )

        with(context) {
            val rightAccessState = initializer?.resolveAccessState() ?: EmptyAccessState

            return data.transformValues { data ->
                val uniquenessState = data.getOrInitialize()
                var newUniquenessState = uniquenessState

                if (initializer != null) {
                    val rightAccessState = initializer.resolveAccessState()
                    val rightUniquenessState = rightAccessState.projectTerminalUniquenessState(uniquenessState)
                    newUniquenessState = newUniquenessState.insert(listOf(leftSymbol), rightUniquenessState)
                }

                newUniquenessState = leftAccessState.initialize(newUniquenessState)

                if (leftSymbol.source?.kind != KtFakeSourceElementKind.WhenGeneratedSubject) {
                    newUniquenessState = rightAccessState.move(newUniquenessState)
                }

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val assignment = node.fir
        val leftValue = assignment.lValue
        val rightValue = assignment.rValue

        with(context) {
            val leftAccessState = leftValue.resolveAccessState()

            return data.transformValues { data ->
                var newUniquenessState = data.getOrInitialize()
                val leftAccessPaths = leftAccessState.enumeratePaths()
                val rightAccessState = rightValue.resolveAccessState()

                if (leftAccessPaths.count() == 1) {
                    val leftPath = leftAccessPaths.first()
                    val rightUniquenessState = rightAccessState.projectTerminalUniquenessState(newUniquenessState)
                    newUniquenessState = newUniquenessState.insert(leftPath, rightUniquenessState)
                }

                newUniquenessState = leftAccessState.initialize(newUniquenessState)
                newUniquenessState = rightAccessState.move(newUniquenessState)

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitFunctionCallEnterNode(
        node: FunctionCallEnterNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val call = node.fir


        with(context) {
            if (uniquenessNeutralCallPredicate.accepts(call)) return data

            return data.transformValues { data ->
                var newUniquenessState = data.getOrInitialize()

                // NOTE: `allReceiverExpressions` also includes context arguments.
                for (receiver in call.allReceiverExpressions) {
                    newUniquenessState = receiver.resolveAccessState().move(newUniquenessState)
                }

                for (argument in call.arguments) {
                    newUniquenessState = argument.resolveAccessState().move(newUniquenessState)
                }

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitFunctionCallExitNode(
        node: FunctionCallExitNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val call = node.fir

        with(context) {
            if (uniquenessNeutralCallPredicate.accepts(call)) return data

            return data.transformValues { data ->
                var newUniquenessState = data.getOrInitialize()
                val explicitReceiver = call.explicitReceiver
                val receiverParameterSymbol = call.toResolvedCallableSymbol()?.receiverParameterSymbol

                if (receiverParameterSymbol != null && explicitReceiver != null && receiverParameterSymbol.resolveLocality() == Locality.Local) {
                    newUniquenessState = explicitReceiver.resolveAccessState().initialize(newUniquenessState)
                }

                for ((argument, requiredLocality) in callArgumentLocalitiesMapper.mapArgumentTypeFactsOf(call)) {
                    if (requiredLocality == Locality.Global) continue

                    newUniquenessState = argument.resolveAccessState().initialize(newUniquenessState)
                }

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitExitDefaultArgumentsNode(
        node: ExitDefaultArgumentsNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val valueParameter = node.fir

        return with(context) {
            data.transformValues { data ->
                var newUniquenessState = data.getOrInitialize()
                val defaultValue = valueParameter.defaultValue ?: return@transformValues data
                val valueParameterSymbol = valueParameter.symbol
                val valueParameterPath = listOf(valueParameterSymbol)
                val defaultValueAccessState = defaultValue.resolveAccessState()
                val defaultValueUniquenessState = defaultValueAccessState.projectTerminalUniquenessState(newUniquenessState)
                newUniquenessState = newUniquenessState.insert(valueParameterPath, defaultValueUniquenessState)
                newUniquenessState = defaultValueAccessState.move(newUniquenessState)
                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitJumpNode(
        node: JumpNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        return when (val jumpExpression = node.fir) {
            is FirReturnExpression -> {
                with (context) {
                    data.transformValues { data ->
                        var newUniquenessState = data.getOrInitialize()
                        val resultAccessState = jumpExpression.result.resolveAccessState()
                        newUniquenessState = resultAccessState.move(newUniquenessState)

                        data.put(Unit, newUniquenessState)
                    }
                }
            }
            else -> { data }
        }
    }

    override fun visitThrowExceptionNode(
        node: ThrowExceptionNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val throwExpression = node.fir

        return with (context) {
            data.transformValues { data ->
                var newUniquenessState = data.getOrInitialize()
                val exceptionAccessState = throwExpression.exception.resolveAccessState()
                newUniquenessState = exceptionAccessState.move(newUniquenessState)

                data.put(Unit, newUniquenessState)
            }
        }
    }
}

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterValueParameterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitSafeCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.INVALID_MOVED_ACCESS

/**
 * Resolves expressions that read paths from the uniqueness state at this CFG node.
 *
 * Currently the expressions are extracted from either [QualifiedAccessNode] and [ExitSafeCallNode], as both node types
 * may represent a field access.
 */
private fun CFGNode<*>.resolveAccess(): FirExpression? =
    when (this) {
        is QualifiedAccessNode -> fir
        is ExitSafeCallNode -> fir
        else -> null
    }

/**
 * Returns the nodes of [this] graph while expanding [EnterValueParameterNode]s to the nodes of their default-argument
 * subgraphs.
 */
private val ControlFlowGraph.nodesIncludingDefaultParameters: Sequence<CFGNode<*>>
    get() = nodes.asSequence().flatMap { node ->
        if (node is EnterValueParameterNode) {
            node.subGraphs.asSequence().flatMap { subGraph -> subGraph.nodes.asSequence() }
        } else {
            sequenceOf(node)
        }
    }

/**
 * Checks that expressions do not read paths that have already been moved.
 */
object FunctionUseAfterMoveChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        val uniquenessStateFlows = lazy { graph.resolveUniquenessStateFlows() }

        for (node in graph.nodesIncludingDefaultParameters) {
            if (node.isDead) continue
            val accessExpression = node.resolveAccess() ?: continue
            val accessState = accessExpression.resolveAccessState()
            val uniquenessState = uniquenessStateFlows.value.readInputUniquenessStateOf(node)
                ?: EmptyUniquenessState

            if (accessState.projectTerminalUniqueness(uniquenessState) == Uniqueness.Moved) {
                reporter.reportOn(accessExpression.source, INVALID_MOVED_ACCESS)
            }
        }
    }
}

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
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
private fun CFGNode<*>.resolveAccesses(): Sequence<FirExpression> =
    when (this) {
        is QualifiedAccessNode -> sequenceOf(fir)
        is ExitSafeCallNode -> sequenceOf(fir)
        else -> emptySequence()
    }

/**
 * Checks that expressions do not read paths that have already been moved.
 */
object FunctionUseAfterMoveChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        val uniquenessStateFlows = lazy { graph.resolveUniquenessStateFlows() }

        for (node in graph.collectLocalNodes()) {
            if (node.isDead) continue

            for (accessExpression in node.resolveAccesses()) {
                val accessState = accessExpression.resolveAccessState()
                val uniquenessState = uniquenessStateFlows.value.readInputUniquenessStateOf(node)
                    ?: EmptyUniquenessState

                if (accessState.projectTerminalUniqueness(uniquenessState) == Uniqueness.Moved) {
                    reporter.reportOn(accessExpression.source, INVALID_MOVED_ACCESS)
                }
            }
        }
    }
}

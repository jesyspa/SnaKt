package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.allReceiverExpressions
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ThrowExceptionNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.ESCAPE_UNIQUENESS_INCONSISTENCY
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.CONTEXT_ESCAPE_UNIQUENESS_INCONSISTENCY

/**
 * Resolves expressions that leave the current function state through this CFG node.
 */
fun CFGNode<*>.resolveEscapes(): Sequence<FirExpression> =
    when (this) {
        is ThrowExceptionNode -> sequenceOf(fir.exception)

        is JumpNode -> {
            val result = (fir as? FirReturnExpression)?.result ?: return emptySequence()
            sequenceOf(result)
        }

        is FunctionCallEnterNode -> fir.allReceiverExpressions.asSequence() + fir.arguments

        else -> emptySequence()
    }

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportEscapeUniquenessInconsistency(
    ownerElement: FirElement,
    escapingExpression: FirExpression,
    inconsistentPath: Path,
) {
    if (escapingExpression.source?.kind is KtFakeSourceElementKind.ImplicitContextParameterArgument) {
        reporter.reportOn(
            ownerElement.source,
            CONTEXT_ESCAPE_UNIQUENESS_INCONSISTENCY,
            escapingExpression.resolvedType,
            inconsistentPath
        )
    } else {
        reporter.reportOn(
            escapingExpression.source ?: ownerElement.source,
            ESCAPE_UNIQUENESS_INCONSISTENCY,
            inconsistentPath
        )
    }
}

/**
 * Checks that escaping references do not contain moved paths.
 *
 * An escaping reference can be defined as a reference leaving the scope of the current function. If a subpath of such
 * reference is moved this represents an inconsistency.
 */
object FunctionEscapeUniquenessConsistencyChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        val uniquenessStateFlows = graph.resolveUniquenessStateFlows()

        for (node in graph.nodes) {
            if (node.isDead) continue

            val inputUniquenessState = lazy {
                uniquenessStateFlows.readInputUniquenessStateOf(node) ?: EmptyUniquenessState
            }

            for (escapingExpression in node.resolveEscapes()) {
                val escapeAccessState = escapingExpression.resolveAccessState()

                for (accessPath in escapeAccessState.enumeratePaths()) {
                    val uniquenessSubstate = inputUniquenessState.value.find(accessPath) ?: continue

                    for (movedPath in uniquenessSubstate.enumerateInconsistentPaths()) {
                        reportEscapeUniquenessInconsistency(
                            node.fir,
                            escapingExpression,
                            accessPath + movedPath
                        )
                    }
                }
            }
        }
    }
}

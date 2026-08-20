package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.declarations.utils.memberDeclarationNameOrNull
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphRenderOptions
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.renderTo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

private fun FirBasedSymbol<*>.render(): String =
    memberDeclarationNameOrNull?.asString() ?: toString()

private fun UniquenessState?.renderOrNull(): String =
    this?.render() ?: "null"

private fun UniquenessState.render(): String? =
    children.takeIf { it.isNotEmpty() }?.entries
        ?.flatMap { (symbol, state) ->
            // A summary node covers everything below it, so its line has to say so: it has no children to render.
            val descendants = if (state.summarizesDescendants) " (and below)" else ""
            val current = "${symbol.render()} ${UniquenessRenderer.render(state.data)}$descendants"
            val children = state.render()?.prependIndent("    ")
            listOfNotNull(current, children)
        }
        ?.joinToString("\n")

/**
 * This method creates the uniqueness-related content for a single CFG Node. To not overload the graph with data, not
 * everything is displayed.
 *
 * In these cases some flow information is added:
 * - The flow changed when executing the node: In and Out flow are displayed
 * - On the border of the function the flow is displayed
 * - On nodes that join multiple flows the flow is displayed
 */
fun CFGNode<*>.render(uniquenessStateFlows: Map<CFGNode<*>, PathAwareUniquenessStateFlow>): String {
    val flowBefore = uniquenessStateFlows.readInputUniquenessStateOf(this)
    val flowAfter = uniquenessStateFlows.readOutputUniquenessStateOf(this)
    val renderedFlowBefore = flowBefore.renderOrNull()
    val renderedFlowAfter = flowAfter.renderOrNull()
    return buildString {
        when {
            // flows differ
            renderedFlowBefore != renderedFlowAfter -> {
                appendLine("Before:")
                appendLine(renderedFlowBefore)
                appendLine()
                appendLine("After:")
                appendLine(renderedFlowAfter)
            }
            // function boundary start
            this@render is FunctionEnterNode -> {
                appendLine("Initial:")
                appendLine(renderedFlowBefore)
            }
            // function boundary end
            this@render is FunctionExitNode -> {
                appendLine("Final:")
                appendLine(renderedFlowAfter)
            }
            // joining node
            this@render.previousNodes.count() > 1 -> {
                appendLine("Merge:")
                appendLine(renderedFlowAfter)
            }
        }
    }
}

/**
 * Renders [this] whole [ControlFlowGraph] showing the intermediate [UniquenessState].
 */
fun ControlFlowGraph.render(uniquenessStateFlows: Map<CFGNode<*>, PathAwareUniquenessStateFlow>): String {
    val options = ControlFlowGraphRenderOptions(
        data = { data: CFGNode<*> -> data.render(uniquenessStateFlows) },
    )
    return buildString {
        renderTo(this, options)
    }
}

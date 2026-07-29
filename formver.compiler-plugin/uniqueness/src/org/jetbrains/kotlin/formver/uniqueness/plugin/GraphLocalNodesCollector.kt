/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterValueParameterNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph

/**
 * Collect the "local" CFG nodes of this graph. A node can be considered local if it is executed in the local scope.
 */
fun ControlFlowGraph.collectLocalNodes(): Sequence<CFGNode<*>> = sequence {
    yieldAll(nodes)

    for (node in nodes) {
        if (node is EnterValueParameterNode) {
            yieldAll(node.fir.controlFlowGraphReference?.controlFlowGraph?.nodes ?: emptyList())
        }
    }
}

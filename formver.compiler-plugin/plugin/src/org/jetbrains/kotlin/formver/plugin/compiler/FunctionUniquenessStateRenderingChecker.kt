/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.formver.uniqueness.plugin.render
import org.jetbrains.kotlin.formver.uniqueness.plugin.resolveUniquenessStateFlows

/**
 * Stub checker for emitting the rendered representation of a graph along with the uniqueness state information.
 */
object FunctionUniquenessStateRenderingChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        if (declaration.origin != FirDeclarationOrigin.Source) return

        val graph = declaration.controlFlowGraphReference?.controlFlowGraph
            ?: return

        val uniquenessStateFlows = graph.resolveUniquenessStateFlows()
        reporter.reportOn(
            declaration.source,
            PluginErrors.UNIQUENESS_CFG,
            graph.render(uniquenessStateFlows)
        )
    }
}

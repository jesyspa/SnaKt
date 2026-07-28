/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.utils.isNonLocal
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

context(context: CheckerContext)
private fun FirFunctionSymbol<*>.declares(symbol: FirBasedSymbol<*>): Boolean {
    val graph = resolvedControlFlowGraphReference?.controlFlowGraph
        ?: return false

    return symbol in graph.resolveDeclaredSymbols()
}

private val FirFunctionSymbol<*>.supportsLocalityCapture: Boolean
    get() = this is FirAnonymousFunctionSymbol || callableId.isLocal

private val FirBasedSymbol<*>.isBoundary: Boolean
    get() = when (this) {
        is FirPropertySymbol, is FirValueParameterSymbol -> false
        else -> true
    }

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        if (expression.resolveLocality() == Locality.Global) return

        val capturedSymbol = expression.calleeReference.symbol ?: return

        for (parentSymbol in context.containingDeclarations.asReversed()) {
            if (!parentSymbol.isBoundary) continue

            if (parentSymbol is FirFunctionSymbol) {
                if (parentSymbol.declares(capturedSymbol)) {
                    return
                } else if (parentSymbol.supportsLocalityCapture) {
                    continue
                }
            }

            reporter.reportOn(
                expression.source,
                LocalityErrors.INVALID_LOCALITY_CAPTURE,
                capturedSymbol,
                parentSymbol,
            )

            return
        }
    }
}

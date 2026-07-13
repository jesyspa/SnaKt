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
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isNonLocal
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

context(context: CheckerContext)
private fun FirFunctionSymbol<*>.declares(symbol: FirBasedSymbol<*>): Boolean {
    val graph = resolvedControlFlowGraphReference?.controlFlowGraph
        ?: return false

    return symbol in graph.resolveDeclaredSymbols()
}

private val FirFunction.supportsLocalityCapture: Boolean
    get() = this is FirAnonymousFunction || !isNonLocal

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        if (expression.resolveLocality() == Locality.Global) return

        val capturedSymbol = expression.calleeReference.symbol ?: return

        var crossedFunctionSymbol: FirBasedSymbol<*>? = null
        var crossedNonFunctionSymbol: FirBasedSymbol<*>? = null

        for (element in context.containingElements.asReversed()) {
            when (element) {
                is FirFunction -> {
                    if (element.symbol.declares(capturedSymbol)) {
                        val crossedBoundarySymbol = crossedNonFunctionSymbol
                            ?: crossedFunctionSymbol
                            ?: return

                        reporter.reportOn(
                            expression.source,
                            LocalityErrors.INVALID_LOCALITY_CAPTURE,
                            capturedSymbol,
                            crossedBoundarySymbol,
                        )
                        return
                    }

                    if (!element.supportsLocalityCapture && crossedFunctionSymbol == null) {
                        crossedFunctionSymbol = element.symbol
                    }
                }

                is FirRegularClass -> {
                    if (crossedNonFunctionSymbol == null) {
                        crossedNonFunctionSymbol = element.symbol
                    }
                }

                is FirAnonymousObject -> {
                    if (crossedNonFunctionSymbol == null) {
                        crossedNonFunctionSymbol = element.symbol
                    }
                }
            }
        }

        reporter.reportOn(
            expression.source,
            LocalityErrors.INVALID_LOCALITY_CAPTURE,
            capturedSymbol,
            crossedNonFunctionSymbol ?: crossedFunctionSymbol ?: FirErrorFunctionSymbol(),
        )
    }
}

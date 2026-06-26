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
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

private fun CheckerContext.collectLocalDeclarations(): Sequence<FirFunctionSymbol<*>> {
    val parentElements = containingElements.asReversed()

    return sequence {
        for (parentElement in parentElements) {
            if (parentElement is FirFunction) {
                val functionSymbol = parentElement.symbol

                yield(functionSymbol)

                if (functionSymbol.resolveLocality() != Locality.Local) break
            }
        }
    }
}

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        if (expression.resolveLocality() == Locality.Global) return

        val symbol = expression.calleeReference.symbol ?: return
        val localDeclarations = context.collectLocalDeclarations()

        if (localDeclarations.any { declaration -> declaration.declares(symbol) }) return

        reporter.reportOn(
            expression.source,
            LocalityErrors.INVALID_LOCALITY_CAPTURE,
            localDeclarations.firstOrNull() ?: FirErrorFunctionSymbol()
        )
    }
}

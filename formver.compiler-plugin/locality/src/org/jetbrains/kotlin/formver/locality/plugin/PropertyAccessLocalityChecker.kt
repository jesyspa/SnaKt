/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

context(context: CheckerContext)
private fun FirFunctionSymbol<*>.declares(symbol: FirBasedSymbol<*>): Boolean =
    when (symbol) {
        is FirReceiverParameterSymbol ->
            symbol.containingDeclarationSymbol == this
        is FirValueParameterSymbol ->
            symbol.containingDeclarationSymbol == this
        is FirLocalPropertySymbol -> {
            val graph = resolvedControlFlowGraphReference?.controlFlowGraph
                ?: return false

            symbol in graph.resolveLocalPropertySymbols()
        }
        else -> false
    }

context(context: CheckerContext)
private fun FirAnonymousFunction.lookupBoundLocality(parentElements: List<FirElement>): Locality {
    for (parentElement in parentElements) {
        when (parentElement) {
            is FirProperty -> {
                val initializer = parentElement.initializer ?: continue

                if (initializer.resolveLambdas().contains(this)) {
                    return parentElement.symbol.resolveLocality()
                }
            }
            is FirCall -> {
                val argumentLocalities = CallArgumentLocalitiesMapper.mapArgumentTypeFactsOf(parentElement)

                for ((expression, locality) in argumentLocalities) {
                    if (this in expression.resolveLambdas()) {
                        return locality
                    }
                }

                if (parentElement is FirQualifiedAccessExpression) {
                    // NOTE: As of now, only extension receivers can be specified as local. It is not possible to
                    // specify other receiver kinds.
                    val extensionReceiver = parentElement.extensionReceiver

                    if (extensionReceiver != null && extensionReceiver.resolveLambdas().contains(this)) {
                        return parentElement.toResolvedCallableSymbol()
                            ?.receiverParameterSymbol?.resolveLocality() ?: Locality.Global
                    }
                }
            }
            is FirFunction -> return Locality.Global
            else -> continue
        }
    }

    return Locality.Global
}

private val CheckerContext.localDeclarations: Sequence<FirFunctionSymbol<*>>
    get() {
        val parentElements = containingElements.asReversed()

        return sequence {
            for ((parentIndex, parentElement) in parentElements.withIndex()) {
                if (parentElement !is FirFunction) continue

                yield(parentElement.symbol)

                if (parentElement is FirAnonymousFunction) {
                    val currentParentElements = parentElements.subList(parentIndex + 1, parentElements.size)
                    val boundLocality = parentElement.lookupBoundLocality(currentParentElements)

                    if (boundLocality != Locality.Local) break
                }
            }
        }
    }

private fun CheckerContext.declaresInLocalScope(symbol: FirBasedSymbol<*>): Boolean =
    localDeclarations.any { declarationSymbol -> declarationSymbol.declares(symbol) }

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val accessSymbol = expression.calleeReference.symbol ?: return

        if (expression.resolveLocality() == Locality.Global) return

        val localDeclarations = context.localDeclarations

        if (context.declaresInLocalScope(accessSymbol)) return

        reporter.reportOn(
            expression.source,
            LocalityErrors.INVALID_LOCALITY_CAPTURE,
            localDeclarations.firstOrNull() ?: FirErrorFunctionSymbol()
        )
    }
}

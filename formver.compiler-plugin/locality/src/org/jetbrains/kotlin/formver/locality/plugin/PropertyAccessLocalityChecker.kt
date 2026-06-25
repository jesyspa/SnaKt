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
private fun FirFunctionSymbol<*>.declares(propertySymbol: FirBasedSymbol<*>): Boolean =
    when (propertySymbol) {
        is FirReceiverParameterSymbol ->
            propertySymbol.containingDeclarationSymbol == this
        is FirValueParameterSymbol ->
            propertySymbol.containingDeclarationSymbol == this
        is FirLocalPropertySymbol -> {
            val graph = resolvedControlFlowGraphReference?.controlFlowGraph ?: return false

            propertySymbol in graph.resolveLocalPropertySymbols()
        }
        else -> false
    }

context(context: CheckerContext)
private fun FirAnonymousFunction.lookupBoundLocality(ancestors: List<FirElement>): Locality {
    for (ancestor in ancestors) {
        when (ancestor) {
            is FirProperty -> {
                val initializer = ancestor.initializer ?: continue

                if (initializer.resolveLambdas().contains(this)) {
                    return ancestor.symbol.resolveLocality()
                }
            }
            is FirCall -> {
                for ((expression, locality) in CallArgumentLocalitiesMapper.mapArgumentTypeFactsOf(ancestor)) {
                    if (this in expression.resolveLambdas()) {
                        return locality
                    }
                }

                if (ancestor is FirQualifiedAccessExpression) {
                    // NOTE: As of now, only extension receivers can be specified as local. It is not possible to
                    // specify other receiver kinds.
                    val extensionReceiver = ancestor.extensionReceiver

                    if (extensionReceiver != null && extensionReceiver.resolveLambdas().contains(this)) {
                        return ancestor.toResolvedCallableSymbol()
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

fun CheckerContext.lookupLocalDeclarations(): Sequence<FirFunctionSymbol<*>> {
    val elements = containingElements.asReversed()

    return sequence {
        for ((index, element) in elements.withIndex()) {
            if (element !is FirFunction) continue

            yield(element.symbol)

            if (element is FirAnonymousFunction) {
                val boundLocality = element.lookupBoundLocality(elements.subList(index + 1, elements.size))

                if (boundLocality != Locality.Local) break
            }
        }
    }
}

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val accessSymbol = expression.calleeReference.symbol ?: return

        if (expression.resolveLocality() == Locality.Global) return

        val localDeclarations = context.lookupLocalDeclarations()
        if (localDeclarations.any { it.declares(accessSymbol) }) return

        reporter.reportOn(
            expression.source,
            LocalityErrors.INVALID_LOCALITY_CAPTURE,
            localDeclarations.firstOrNull() ?: FirErrorFunctionSymbol()
        )
    }
}

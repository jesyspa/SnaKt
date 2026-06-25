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
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.type.plugin.collectTails
import org.jetbrains.kotlin.formver.type.plugin.removeCast

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
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

    private fun FirExpression.tailLambdas(): Sequence<FirAnonymousFunction> {
        val expression = unwrapExpression().removeCast()
        val tails = expression.collectTails()
        return if (tails.none())
            listOfNotNull((expression as? FirAnonymousFunctionExpression)?.anonymousFunction).asSequence()
        else
            tails.flatMap { it.tailLambdas() }
    }

    context(context: CheckerContext)
    private fun FirAnonymousFunction.bindingLocality(ancestors: List<FirElement>): Locality {
        val lambda = this
        for (ancestor in ancestors) {
            when (ancestor) {
                is FirProperty ->
                    if (ancestor.initializer?.tailLambdas()?.any { it === lambda } == true)
                        return ancestor.symbol.resolveLocality()
                is FirCall -> {
                    val parameter = ancestor.resolvedArgumentMapping?.entries
                        ?.firstOrNull { (argument, _) -> argument.tailLambdas().any { it === lambda } }
                        ?.value
                    if (parameter != null) return parameter.symbol.resolveLocality()

                    if (ancestor is FirQualifiedAccessExpression &&
                        ancestor.extensionReceiver?.tailLambdas()?.any { it === lambda } == true
                    ) {
                        ancestor.toResolvedCallableSymbol()?.receiverParameterSymbol
                            ?.let { return it.resolveLocality() }
                    }
                }
                is FirFunction -> return Locality.Global
                else -> {}
            }
        }
        return Locality.Global
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val accessSymbol = expression.calleeReference.symbol ?: return

        if (expression.resolveLocality() == Locality.Global) return

        val elements = context.containingElements.asReversed()
        var innermostFunction: FirFunctionSymbol<*>? = null

        for ((index, element) in elements.withIndex()) {
            if (element !is FirFunction) continue
            if (innermostFunction == null) innermostFunction = element.symbol
            if (element.symbol.declares(accessSymbol)) return
            val crossingIsLocalLambda = element is FirAnonymousFunction &&
                    element.bindingLocality(elements.subList(index + 1, elements.size)) == Locality.Local
            if (!crossingIsLocalLambda) break
        }

        reporter.reportOn(
            expression.source,
            LocalityErrors.INVALID_LOCALITY_CAPTURE,
            innermostFunction ?: FirErrorFunctionSymbol()
        )
    }
}

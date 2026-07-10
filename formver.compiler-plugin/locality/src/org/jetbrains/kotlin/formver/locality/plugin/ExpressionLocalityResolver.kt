/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.formver.locality.contract.plugin.resolveLocalityContract
import org.jetbrains.kotlin.formver.type.plugin.CallArgumentTypeFactsMapper
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeFactResolver
import org.jetbrains.kotlin.formver.type.plugin.InvokeParameterTypeFactsResolver
import org.jetbrains.kotlin.formver.type.plugin.QualifiedAccessArgumentTypeFactMapper
import org.jetbrains.kotlin.formver.type.plugin.ReturnResultTypeFactResolver
import org.jetbrains.kotlin.formver.type.plugin.ThrowExceptionTypeFactResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeFactResolver

private object TerminalLocalityResolver : ExpressionTypeFactResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveTypeFactOf(expression: FirExpression): Locality =
        when (expression) {
            is FirAnonymousFunctionExpression -> {
                val graph = expression.anonymousFunction.controlFlowGraphReference?.controlFlowGraph
                    ?: return Locality.Global

                graph.resolveScopeLocality()
            }

            is FirAnonymousObjectExpression -> {
                val graph = expression.anonymousObject.controlFlowGraphReference?.controlFlowGraph
                    ?: return Locality.Global

                graph.resolveScopeLocality()
            }

            is FirQualifiedAccessExpression ->
                when (val symbol = expression.calleeReference.symbol) {
                    is FirCallableSymbol<*> -> symbol.resolveDeclaredLocality()
                    is FirReceiverParameterSymbol -> symbol.resolveDeclaredLocality()
                    else -> Locality.Global
                }

            else -> Locality.Global
        }
}

class ExpressionLocalityResolver(session: FirSession) :
    ExpressionTypeFactResolver<Locality> by UnifyingExpressionTypeFactResolver(
        session.firCachesFactory,
        LocalityUnifier,
        TerminalLocalityResolver
    ), FirExtensionSessionComponent(session) {
    companion object : ExpressionTypeFactResolver<Locality> {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionLocalityResolver(session) }
        }

        context(context: CheckerContext)
        override fun resolveTypeFactOf(expression: FirExpression): Locality =
            context.session.expressionLocalityResolver.resolveTypeFactOf(expression)
    }
}

private val FirSession.expressionLocalityResolver: ExpressionLocalityResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveLocality(): Locality =
    ExpressionLocalityResolver.resolveTypeFactOf(this)

object ReturnResultLocalityResolver : ReturnResultTypeFactResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveResultTypeFactOf(expression: FirReturnExpression): Locality = Locality.Global
}

object ThrowExceptionLocalityResolver : ThrowExceptionTypeFactResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveExceptionTypeFactOf(expression: FirThrowExpression): Locality = Locality.Global
}

object InvokeParametersLocalityResolver : InvokeParameterTypeFactsResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveInvokeParametersOf(receiver: FirExpression): List<Locality>? =
        receiver.resolveLocalityContract()?.parameterTypeFacts?.map { it.typeFact }
}

val CallArgumentLocalitiesMapper = CallArgumentTypeFactsMapper(
    VariableLocalityResolver,
    InvokeParametersLocalityResolver
)

val QualifiedAccessArgumentLocalitiesMapper = QualifiedAccessArgumentTypeFactMapper(
    ReceiverLocalityResolver,
    VariableLocalityResolver
)

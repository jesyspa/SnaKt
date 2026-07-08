/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeFactResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeFactResolver

/**
 * Resolves the access-state contributed by [this] when it is the terminal expression of a larger expression.
 */
context(context: CheckerContext)
fun FirExpression.resolveTerminalAccessState(): AccessState =
    when (this) {
        is FirQualifiedAccessExpression -> {
            when (val symbol = calleeReference.symbol) {
                is FirReceiverParameterSymbol -> {
                    EmptyAccessState.putChild(symbol, AccessState(Access.Terminal))
                }
                is FirVariableSymbol<*> -> {
                    val receiverState = pathReceiver
                        ?.resolveAccessState()
                        ?: EmptyAccessState

                    receiverState.append(EmptyAccessState.putChild(symbol, AccessState(Access.Terminal)))
                }
                else -> EmptyAccessState
            }
        }
        is FirSafeCallExpression -> {
            val selector = selector

            return if (selector is FirExpression) {
                selector.resolveAccessState()
            } else {
                EmptyAccessState
            }
        }
        else -> EmptyAccessState
    }

/**
 * Resolves the access-state of an expression by joining the access-states of its tail subexpressions.
 */
class ExpressionAccessStateResolver(session: FirSession) :
    FirExtensionSessionComponent(session),
    ExpressionTypeFactResolver<AccessState> by UnifyingExpressionTypeFactResolver(
        session.firCachesFactory,
        AccessState::join,
        { expression -> expression.resolveTerminalAccessState() }
    ) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionAccessStateResolver(session) }
        }
    }
}

private val FirSession.expressionAccessStateResolver: ExpressionAccessStateResolver
        by FirSession.sessionComponentAccessor()

/**
 * Resolves the paths in the current expression that refer to mutable uniqueness state.
 */
context(context: CheckerContext)
fun FirExpression.resolveAccessState(): AccessState {
    return context.session.expressionAccessStateResolver.resolveTypeFactOf(this)
}

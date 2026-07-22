/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirJump
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullLiteral
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeFactResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeFactResolver

/**
 * Resolves uniqueness from the access paths referenced by [this], or [Uniqueness.Shared] if it references no path.
 */
context(context: CheckerContext)
fun FirExpression.resolveAccessUniqueness(): Uniqueness {
    val accessState = resolveAccessState()

    return if (accessState == EmptyAccessState) {
        Uniqueness.Shared
    } else {
        accessState.symbols.fold(Uniqueness.Unique) { result, symbol ->
            result.join(symbol.resolveDeclaredUniqueness())
        }
    }
}

/**
 * Resolves the uniqueness contributed by [this] when it is the terminal expression of a larger expression.
 */
context(context: CheckerContext)
fun FirExpression.resolveTerminalUniqueness(): Uniqueness {
    return when (this) {
        is FirFunctionCall -> {
            if (calleeReference.symbol is FirConstructorSymbol) {
                Uniqueness.Unique
            } else {
                resolvedType.scopeUniqueness
            }
        }

        is FirThisReceiverExpression -> {
            resolveAccessUniqueness()
        }

        is FirPropertyAccessExpression -> {
            val receiverUniqueness = pathReceiver?.resolveUniqueness() ?: Uniqueness.Unique

            receiverUniqueness.join(resolveAccessUniqueness())
        }

        is FirSafeCallExpression -> {
            val receiver = receiver
            val receiverUniqueness = receiver.resolveUniqueness()

            receiverUniqueness.join(resolveAccessUniqueness())
        }

        is FirLiteralExpression -> {
            if (isNullLiteral) {
                Uniqueness.Unique
            } else {
                Uniqueness.Shared
            }
        }

        is FirJump<*>, is FirThrowExpression -> Uniqueness.Unique

        else -> Uniqueness.Shared
    }
}

/**
 * Resolves the declared uniqueness of an expression.
 *
 * NOTE: This resolver is flow-insensitive
 */
class ExpressionUniquenessResolver(session: FirSession) :
    ExpressionTypeFactResolver<Uniqueness> by UnifyingExpressionTypeFactResolver(
        session.firCachesFactory,
        UniquenessUnifier,
        { expression -> expression.resolveTerminalUniqueness() }
    ), FirExtensionSessionComponent(session) {
    companion object : ExpressionTypeFactResolver<Uniqueness> {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionUniquenessResolver(session) }
        }

        context(context: CheckerContext)
        override fun resolveTypeFactOf(expression: FirExpression): Uniqueness =
            expression.resolveUniqueness()
    }
}

private val FirSession.expressionUniquenessResolver: ExpressionUniquenessResolver
        by FirSession.sessionComponentAccessor()

/**
 * Resolves the declared, flow-insensitive uniqueness of [this] expression.
 */
context(context: CheckerContext)
fun FirExpression.resolveUniqueness(): Uniqueness =
    context.session.expressionUniquenessResolver.resolveTypeFactOf(this)

/**
 * Resolves the uniqueness expected for the result of [this] return expression.
 */
fun FirReturnExpression.resolveResultUniqueness(): Uniqueness =
    target.labeledElement.returnTypeRef.coneType.scopeUniqueness

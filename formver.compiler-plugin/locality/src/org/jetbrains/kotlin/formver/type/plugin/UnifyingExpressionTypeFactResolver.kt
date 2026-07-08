/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.lastExpression
import org.jetbrains.kotlin.utils.yieldIfNotNull

/**
 * Strips [FirExpression] from cast operations.
 */
fun FirExpression.removeCast(): FirExpression =
    when (this) {
        is FirTypeOperatorCall -> when (operation) {
            FirOperation.AS, FirOperation.SAFE_AS ->
                argument.unwrapExpression().removeCast()
            else -> this
        }
        else -> this
    }

/**
 * Collects all the tails of a conditional expression. Note that these tails do not include the expression itself. If
 * the expression is not conditional it returns an empty sequence.
 */
fun FirExpression.collectTails(): Sequence<FirExpression> =
    when (val expression = unwrapExpression()) {
        is FirWhenExpression ->
            sequence {
                yieldAll(expression.branches.map(FirWhenBranch::result))
            }
        is FirElvisExpression ->
            sequence {
                yield(expression.lhs)
                yield(expression.rhs)
            }
        is FirTryExpression ->
            sequence {
                yield(expression.tryBlock)
                yieldAll(expression.catches.map(FirCatch::block))
            }
        is FirBlock ->
            sequence {
                yieldIfNotNull(expression.lastExpression)
            }
        else ->
            emptySequence()
    }

/**
 * Resolves the type fact of an expression by unifying the type facts of its tail subexpressions.
 *
 * @param TypeFact the type-fact class of the expression.
 * @param cachesFactory the factory for creating the cache for storing the subexpression results.
 * @param typeFactUnifier the type-fact unifier to use for unifying the type facts of the expression tails.
 * @param terminalTypeFactResolver the resolver for resolving the type fact of the expression if it has no tail
 *  subexpressions.
 */
class UnifyingExpressionTypeFactResolver<TypeFact>(
    cachesFactory: FirCachesFactory,
    private val typeFactUnifier: TypeFactUnifier<TypeFact>,
    private val terminalTypeFactResolver: ExpressionTypeFactResolver<TypeFact>
) : ExpressionTypeFactResolver<TypeFact> {
    private val cache = cachesFactory.createCache { expression: FirExpression, context: CheckerContext ->
        with(context) {
            extractTypeFactOf(expression)
        }
    }

    context(context: CheckerContext)
    override fun resolveTypeFactOf(expression: FirExpression): TypeFact =
        cache.getValue(expression, context)

    context(context: CheckerContext)
    private fun extractTypeFactOf(expression: FirExpression): TypeFact {
        val expression = expression.unwrapExpression().removeCast()
        val tails = expression.collectTails()

        if (!tails.any()) {
            return terminalTypeFactResolver.resolveTypeFactOf(expression)
        } else {
            return tails.map { resolveTypeFactOf(it) }.reduce(typeFactUnifier::join)
        }
    }
}

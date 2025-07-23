/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.And
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.Or
import org.jetbrains.kotlin.formver.embeddings.types.buildType
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.linearization.LogicOperatorPolicy
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * In pure contexts these operators can be written as simple binary operators.
 * However, regularly their semantics is different: evaluate the first argument and then maybe the second one (not necessarily)
 */
sealed class SequentialLogicOperatorEmbedding: BinaryDirectResultExpEmbedding {
    override val type
        get() = buildType { boolean() }

    protected abstract val ifReplacement: ExpEmbedding
    context(nameResolver: NameResolver)
    protected abstract val expressionReplacement: ExpEmbedding
    context(nameResolver: NameResolver)
    private fun operatorReplacement(ctx: LinearizationContext) = when (ctx.logicOperatorPolicy) {
        LogicOperatorPolicy.CONVERT_TO_IF -> ifReplacement
        LogicOperatorPolicy.CONVERT_TO_EXPRESSION -> expressionReplacement
    }
    context(nameResolver: NameResolver)
    override fun toViper(ctx: LinearizationContext): Exp =
        operatorReplacement(ctx).toViper(ctx)

    context(nameResolver: NameResolver)
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        operatorReplacement(ctx).toViperBuiltinType(ctx)

    context(nameResolver: NameResolver)
    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        operatorReplacement(ctx).toViperStoringIn(result, ctx)
    }
}

class SequentialAnd(override val left: ExpEmbedding, override val right: ExpEmbedding) : SequentialLogicOperatorEmbedding() {
    override val ifReplacement
        get() = If(left, right, BooleanLit(false), buildType { boolean() })
    context(nameResolver: NameResolver)
    override val expressionReplacement
        get() = And(left, right)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitSequentialAnd(this)
}

class SequentialOr(override val left: ExpEmbedding, override val right: ExpEmbedding) : SequentialLogicOperatorEmbedding() {
    override val ifReplacement
        get() = If(left, BooleanLit(true), right, buildType { boolean() })
    context(nameResolver: NameResolver)
    override val expressionReplacement
        get() = Or(left, right)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitSequentialOr(this)
}
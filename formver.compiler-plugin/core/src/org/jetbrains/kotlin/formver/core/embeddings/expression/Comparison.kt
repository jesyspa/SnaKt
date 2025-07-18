/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.EqAny
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.NeAny
import org.jetbrains.kotlin.formver.viper.ast.Operator

sealed interface AnyComparisonExpression : BinaryDirectResultExpEmbedding {
    override val type
        get() = buildType { boolean() }

    val comparisonOperation: Operator

    override fun toViper(ctx: LinearizationContext): Exp =
        RuntimeTypeDomain.boolInjection.toRef(
            toViperBuiltinType(ctx),
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo
        )

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        // this check guarantees that arguments will be of the same Viper type
        if (left.type == right.type)
            comparisonOperation(
                left.toViperBuiltinType(ctx),
                right.toViperBuiltinType(ctx),
                pos = ctx.source.asPosition,
                info = sourceRole.asInfo
            )
        else comparisonOperation(
            left.toViper(ctx),
            right.toViper(ctx),
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo
        )
}

data class EqCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : AnyComparisonExpression {
    override val comparisonOperation = EqAny
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitEqCmp(this)
}

data class NeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : AnyComparisonExpression {

    override val comparisonOperation = NeAny
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitNeCmp(this)
}

fun ExpEmbedding.notNullCmp(): ExpEmbedding = NeCmp(this, NullLit)


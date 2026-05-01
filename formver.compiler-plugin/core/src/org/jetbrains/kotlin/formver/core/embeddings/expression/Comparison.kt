/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.viper.ast.EqAny
import org.jetbrains.kotlin.formver.viper.ast.NeAny
import org.jetbrains.kotlin.formver.viper.ast.Operator

sealed interface AnyComparisonExpression : ExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding

    override val type
        get() = buildType { boolean() }

    val comparisonOperation: Operator

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)
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

/**
 * Reference equality (Kotlin `===`).
 *
 * Unlike [EqCmp], this never unwraps to a builtin type — operands are compared
 * as Viper `Ref`s, so two distinct boxed values are not identified even when
 * their unwrapped representations match.
 */
data class IdentityCmp(
    val left: ExpEmbedding,
    val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : ExpEmbedding {
    override val type
        get() = buildType { boolean() }

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIdentityCmp(this)
}

fun ExpEmbedding.notNullCmp(): ExpEmbedding = NeCmp(this, NullLit)

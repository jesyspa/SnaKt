/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.purity.PurityContext
import org.jetbrains.kotlin.formver.core.purity.isPure

data class ForAllEmbedding(
    // TODO: support multiple variables
    val variable: VariableEmbedding,
    val conditions: List<ExpEmbedding>,
    val triggerExpressions: List<ExpEmbedding> = emptyList(),
) : ExpEmbedding {

    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun children(): Sequence<ExpEmbedding> = conditions.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitForAllEmbedding(this)

    override fun isValid(ctx: PurityContext): Boolean {
        val allPure = conditions.all { it.isPure() }
        if (!allPure) ctx.addPurityError(this, "forAll body contains impure expressions; method calls are not supported in quantifier bodies")
        return allPure
    }
}

data class ExistsEmbedding(
    val variable: VariableEmbedding,
    val conditions: List<ExpEmbedding>,
    val triggerExpressions: List<ExpEmbedding> = emptyList(),
) : ExpEmbedding {

    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun children(): Sequence<ExpEmbedding> = conditions.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitExistsEmbedding(this)

    override fun isValid(ctx: PurityContext): Boolean {
        val allPure = conditions.all { it.isPure() }
        if (!allPure) ctx.addPurityError(this, "exists body contains impure expressions; method calls are not supported in quantifier bodies")
        return allPure
    }
}

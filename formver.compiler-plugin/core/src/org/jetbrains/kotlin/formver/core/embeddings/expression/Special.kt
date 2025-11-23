/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.core.purity.ExprPurityVisitor
import org.jetbrains.kotlin.formver.core.purity.PurityContext
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Especially when working with type information, there are a number of expressions that do not have a corresponding `ExpEmbedding`.
 * We will eventually want to solve this somehow, but there are still open design questions there, so for now this wrapper will
 * do the job.
 */
data class ExpWrapper(val value: Exp, override val type: TypeEmbedding) : PureExpEmbedding {
    override fun toViper(source: KtSourceElement?): Exp = value
}

data object ErrorExp : NoResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { nothing() }
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        ctx.addStatement { Stmt.Inhale(Exp.BoolLit(false, ctx.source.asPosition), ctx.source.asPosition) }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitErrorExp(this)
}

data class Assert(val exp: ExpEmbedding) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addStatement { Stmt.Assert(exp.toViperBuiltinType(ctx), ctx.source.asPosition) }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(exp)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAssert(this)

    override fun isValid(ctx: PurityContext): Boolean = exp.accept(ExprPurityVisitor).also {
        if (!it) ctx.addPurityError(exp, "Assert condition is impure")
    }
}

/**
 * Immediately performs an unconditional inhale of the statement.
 *
 * This can cause all kinds of issues with statement ordering, so it's more of a solution for porting legacy stuff than something
 * we should be adding more of going forward.
 */
data class InhaleDirect(val exp: ExpEmbedding) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addStatement { Stmt.Inhale(exp.toViperBuiltinType(ctx), ctx.source.asPosition) }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitInhaleDirect(this)
}


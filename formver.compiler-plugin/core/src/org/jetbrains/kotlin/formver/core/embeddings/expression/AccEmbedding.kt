/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp

class AccEmbedding(
    val field: Exp.FieldAccess,
    val perm: PermExp,
) : OnlyToBuiltinTypeExpEmbedding {

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
        return Exp.Acc(
            field = field,
            perm = perm,
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo,
        )
    }

    override val subexpressions: List<ExpEmbedding> = TODO()

    override val type: TypeEmbedding
        get() = TODO()

    override fun <R> accept(v: ExpVisitor<R>): R = TODO()
}
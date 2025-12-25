/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.backend.jvm.AccessType
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FieldAccessTypeInvariantEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp

enum class PermOption{
    READ, WRITE
}

class AccEmbedding(
    val field: FieldEmbedding,
    val access: ExpEmbedding,
    val perm: PermOption,
) : OnlyToBuiltinTypeExpEmbedding {
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
        val field = Exp.FieldAccess(
            access.toViper(ctx),
            field.toViper(),
            ctx.source.asPosition,
        )
        val permission = if (perm == PermOption.READ) PermExp.WildcardPerm() else PermExp.FullPerm()
        return Exp.Acc(
            field = field,
            perm = permission,
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo,
        )
    }

    override val subexpressions: List<ExpEmbedding> = listOf(access)

    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAccEmbedding(this)
}
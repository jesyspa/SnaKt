/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

data class Old(override val inner: ExpEmbedding) : UnaryDirectResultExpEmbedding {
    override val type: TypeEmbedding = inner.type
    override fun toViper(ctx: LinearizationContext): Exp = Exp.Old(inner.toViper(ctx), ctx.source.asPosition)
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        Exp.Old(inner.toViperBuiltinType(ctx), ctx.source.asPosition)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitOld(this)
}

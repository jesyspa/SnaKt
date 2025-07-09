/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.embeddings.types.injection
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.viperLiteral

interface LiteralEmbedding : PureExpEmbedding {
    val value: Any?

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(PlaintextLeaf(value.toString()))

    override fun toViper(source: KtSourceElement?): Exp =
        type.injection.toRef(
            value.viperLiteral(source.asPosition, sourceRole.asInfo),
            pos = source.asPosition,
            info = sourceRole.asInfo,
        )
}

data object UnitLit : LiteralEmbedding, UnitResultExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp =
        super<UnitResultExpEmbedding>.toViper(ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) =
        super<UnitResultExpEmbedding>.toViperUnusedResult(ctx)

    // No operation: we just want to return unit.
    override fun toViperSideEffects(ctx: LinearizationContext) = Unit

    override val value = Unit

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitUnitLit(this)
}

data class IntLit(override val value: Int) : LiteralEmbedding {
    override val type = buildType { int() }

    override val debugName = "Int"
}

data class BooleanLit(
    override val value: Boolean,
    override val sourceRole: SourceRole? = null
) : LiteralEmbedding {

    override val type = buildType { boolean() }

    override val debugName = "Boolean"
}

data class CharLit(
    override val value: Char,
) : LiteralEmbedding {
    override val type = buildType { char() }

    override val debugName: String = "Char"
}

data class StringLit(
    override val value: String,
) : LiteralEmbedding {
    override val type = buildType { string() }

    override val debugName: String = "String"
}

data object NullLit : LiteralEmbedding {
    override val value = null

    override val type = buildType { isNullable = true; nothing() }
    override fun toViper(source: KtSourceElement?): Exp =
        RuntimeTypeDomain.nullValue(pos = source.asPosition)

    override val debugName = "Null"
}


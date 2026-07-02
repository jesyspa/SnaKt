/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.viper.ast.PermExp

data class AccEmbedding(
    val receiver: ExpEmbedding,
    val field: FieldEmbedding,
    val perm: PermExp,
) : ExpEmbedding {
    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAccEmbedding(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(receiver)
}

data class PermissionLit(val perm: PermExp) : ExpEmbedding {
    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPermissionLit(this)
    override fun children(): Sequence<ExpEmbedding> = emptySequence()
}

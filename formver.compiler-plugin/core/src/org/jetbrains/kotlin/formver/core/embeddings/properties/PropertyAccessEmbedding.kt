/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

interface PropertyAccessEmbedding {
    fun getValue(ctx: StmtConversionContext): ExpEmbedding
    fun setValue(value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding
}

fun ExpEmbedding.asPropertyAccess() = when (this) {
    is PropertyAccessEmbedding -> this
    else -> object : PropertyAccessEmbedding {
        override fun getValue(ctx: StmtConversionContext): ExpEmbedding =
            this@asPropertyAccess

        override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding {
            error("Property does not have a settable value")
        }
    }
}
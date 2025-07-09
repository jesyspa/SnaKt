/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings

object LengthFieldGetter : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext) =
        OperatorExpEmbeddings.StringLength(receiver)
}
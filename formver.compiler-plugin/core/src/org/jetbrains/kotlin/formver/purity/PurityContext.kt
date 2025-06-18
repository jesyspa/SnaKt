/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.purity

import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding

interface PurityContext {
    fun addPurityError(embedding: ExpEmbedding, msg: String)
}

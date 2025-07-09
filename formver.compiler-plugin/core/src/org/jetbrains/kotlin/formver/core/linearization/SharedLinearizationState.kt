/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding

class SharedLinearizationState(private val producer: FreshEntityProducer<AnonymousVariableEmbedding, TypeEmbedding>) {
    fun freshAnonVar(type: TypeEmbedding) = producer.getFresh(type)
}
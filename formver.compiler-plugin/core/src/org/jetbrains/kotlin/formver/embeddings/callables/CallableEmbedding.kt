/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.withNewTypeInvariants
import org.jetbrains.kotlin.formver.embeddings.types.FunctionTypeEmbedding

/**
 * Kotlin entity that can be called.
 */
interface CallableEmbedding {
    val callableType: FunctionTypeEmbedding
    fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding
}

fun CallableEmbedding.insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext, actualReturnType: TypeEmbedding) =
    insertCall(args, ctx).withNewTypeInvariants(actualReturnType) {
        access = true
        proven = true
    }
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withNewTypeInvariants
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding

/**
 * Kotlin entity that can be called.
 */
interface CallableEmbedding {
    val callableType: FunctionTypeEmbedding

    /**
     * Whether [insertCall] expects one argument per formal argument of the Kotlin callable:
     * receivers first, then one per value parameter, in declaration order.
     *
     * `false` for callables that interpret whatever arguments the call site provides; those get no
     * argument for a parameter left at its default value.
     */
    val takesArgumentPerParameter: Boolean

    fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding
}

fun CallableEmbedding.insertCall(
    args: List<ExpEmbedding>,
    ctx: StmtConversionContext,
    actualReturnType: TypeEmbedding
) =
    insertCall(args, ctx).withNewTypeInvariants(actualReturnType, ctx.typeResolver) {
        access = true
        proven = true
    }

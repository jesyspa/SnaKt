/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Method

/**
 * Some functions are handled specially by our conversion.
 * This can be helpful when proving. For example, `Int.plus(Int)` will
 * eventually be translated to native operator `+` in Viper.
 *
 * In particular, that means that there won't be a call to the Viper method
 * sometimes.
 */
interface SpecialKotlinFunction : FunctionEmbedding {
    val packageName: List<String>
    val className: String?
        get() = null
    val name: String
}

/**
 * Kotlin function that will always be handled specially, like aforementioned `Int.plus(Int)`.
 */
interface FullySpecialKotlinFunction : SpecialKotlinFunction {
    override val viperMethod: Method?
        get() = null
}

/**
 * Kotlin function that will sometimes be handled specially depending on arguments they're called with.
 *
 * This is useful in cases like `String.plus(Any)`. If `Any` turns out to be a `String` we can substitute
 * sequence concatenation in Viper. Otherwise, we're forced to call an actual method (which is stored in `baseEmbedding`).
 *
 * Currently, `String.plus(Any)` is the sole case when we use this interface.
 */
interface PartiallySpecialKotlinFunction : SpecialKotlinFunction {
    /**
     * `baseEmbedding` stores a usual (user) embedding for the partially special function.
     * It is initialised iff the partially special function is used in the program in any way.
     */
    val baseEmbedding: FunctionEmbedding?
    fun tryInsertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding?
    override fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding {
        return tryInsertCall(args, ctx) ?: baseEmbedding?.insertCall(args, ctx)
        ?: error("Base embedding for partially special function $name not specified")
    }

    fun initBaseEmbedding(embedding: FunctionEmbedding)

    override val viperMethod: Method?
        get() = baseEmbedding?.viperMethod
}
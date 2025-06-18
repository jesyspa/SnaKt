/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.AddStringChar
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.AddStringString
import org.jetbrains.kotlin.formver.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.embeddings.types.equalToType
import org.jetbrains.kotlin.formver.embeddings.types.nullableAny
import org.jetbrains.kotlin.formver.viper.MangledName

/**
 * Base class for implementations of `PartiallySpecialKotlinFunction`s.
 */
abstract class AbstractPartiallySpecialKotlinFunction(
    vararg packageName: String,
    override val className: String? = null,
    override val name: String,
) : PartiallySpecialKotlinFunction {
    private var _baseEmbedding: FunctionEmbedding? = null
    override val baseEmbedding
        get() = _baseEmbedding

    override val packageName = packageName.toList()

    override fun initBaseEmbedding(embedding: FunctionEmbedding) {
        check(_baseEmbedding == null) { "Base embedding for partially special function $name already initialized." }
        _baseEmbedding = embedding
    }
}

class StringPlusAnyFunction : AbstractPartiallySpecialKotlinFunction("kotlin", className = "String", name = "plus") {
    override fun tryInsertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding? {
        val argType = args[1].type
        return when {
            argType.equalToType { string() } -> AddStringString(args[0], args[1])
            argType.equalToType { char() } -> AddStringChar(args[0], args[1])
            else -> null
        }
    }

    override val callableType = buildFunctionPretype {
        withDispatchReceiver { string() }
        withParam { nullableAny() }
        withReturnType { string() }
    }
}

/**
 * Here all the partially special functions are stored.
 * Since a partially special function has a state (its base embedding) we can't use a static variable here,
 * but those functions are generated once per run anyway.
 */
object PartiallySpecialKotlinFunctions {
    fun generateAllByName(): Map<MangledName, FunctionEmbedding> =
        buildList {
            add(StringPlusAnyFunction())
        }.associateBy { it.embedName() }
}


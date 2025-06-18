/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.UnitLit
import org.jetbrains.kotlin.formver.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName

class FullySpecialKotlinFunctionImpl(
    override val packageName: List<String>,
    override val className: String?,
    override val name: String,
    override val callableType: FunctionTypeEmbedding,
    val body: (List<ExpEmbedding>, StmtConversionContext) -> ExpEmbedding,
) : FullySpecialKotlinFunction {
    override fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext) =
        body(args, ctx)
}

class FullySpecialKotlinFunctionBuilder {
    private val byName = mutableMapOf<MangledName, FunctionEmbedding>()

    fun withCallableType(
        callableType: FunctionTypeEmbedding,
        functionsBlock: SpecialKotlinFunctionBuilderWithCallableType.() -> Unit,
    ) {
        val builderWithCallableType = SpecialKotlinFunctionBuilderWithCallableType(callableType)

        builderWithCallableType.functionsBlock()
    }

    fun addFunction(
        callableType: FunctionTypeEmbedding,
        packageName: List<String>,
        className: String? = null,
        name: String,
        body: (List<ExpEmbedding>, StmtConversionContext) -> ExpEmbedding,
    ) {
        withCallableType(callableType) {
            addFunction(packageName, className = className, name = name, body = body)
        }
    }

    inner class SpecialKotlinFunctionBuilderWithCallableType(private val callableType: FunctionTypeEmbedding) {

        fun addFunction(
            packageName: List<String>,
            className: String? = null,
            name: String,
            body: (List<ExpEmbedding>, StmtConversionContext) -> ExpEmbedding,
        ) {
            FullySpecialKotlinFunctionImpl(packageName, className, name, callableType, body).apply { byName[embedName()] = this }
        }

        fun addNoOpFunction(
            packageName: List<String>,
            className: String? = null,
            name: String,
        ) = addFunction(packageName, className, name) { _, _ ->
            UnitLit
        }
    }

    fun complete(): Map<MangledName, FunctionEmbedding> = byName
}

fun buildFullySpecialFunctions(functionsBlock: FullySpecialKotlinFunctionBuilder.() -> Unit): Map<MangledName, FunctionEmbedding> =
    FullySpecialKotlinFunctionBuilder().apply(functionsBlock).complete()


/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.domains.InjectionImageFunction
import org.jetbrains.kotlin.formver.embeddings.types.*
import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * Builder to generate `ExpEmbedding`s with 1 or 2 arguments directly from their Viper implementation.
 *
 * Example: Let's imagine we want to create an `ExpEmbedding` to sum two integers.
 * Then we should just set its signature (`(Int, Int) -> Int`) and Viper implementation (`Exp.Add`).
 */
class OperatorExpEmbeddingBuilder {
    private var runtimeTypeFunctionName: String? = null
    private var viperApplicable: Applicable? = null
    private var callableType: FunctionTypeEmbedding? = null
    private var additionalConditions: (FunctionBuilder.() -> Unit)? = null

    fun complete(): OperatorExpEmbeddingTemplate? {
        val callableType = callableType ?: error("Signature not specified to buildExpEmbedding.")
        val argumentTypes = callableType.formalArgTypes
        val returnType = callableType.returnType
        val refsOperation = InjectionImageFunction(
            runtimeTypeFunctionName ?: error("No name specified for the underlying viper function."),
            viperApplicable ?: error("No viper operator specified to build ExpEmbedding."),
            argumentTypes.map { it.injection },
            returnType.injection,
            additionalConditions ?: { }
        )
        return OperatorExpEmbeddingTemplate.create(returnType, refsOperation)
    }

    fun setName(name: String) {
        check(runtimeTypeFunctionName == null) { "Name for underlying viper function is already set." }
        runtimeTypeFunctionName = name
    }

    fun setSignature(signature: FunctionTypeEmbedding) {
        check(callableType == null) { "Signature to build ExpEmbedding is already set." }
        callableType = signature
    }

    fun withSignature(block: FunctionPretypeBuilder.() -> Unit) {
        setSignature(buildFunctionPretype(block))
    }

    data class ViperCallData(
        val args: List<Exp>,
        val pos: Position,
        val info: Info,
        val trafos: Trafos,
    )

    fun viperImplementation(block: ViperCallData.() -> Exp) {
        check(viperApplicable == null) { "Viper implementation for OperatorExpEmbedding is already set." }
        viperApplicable = object : Applicable {
            override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
                return ViperCallData(args, pos, info, trafos).block()
            }
        }
    }

    fun additionalConditions(block: FunctionBuilder.() -> Unit) {
        additionalConditions = block
    }
}

inline fun <reified T : OperatorExpEmbeddingTemplate> buildOperator(block: OperatorExpEmbeddingBuilder.() -> Unit) =
    when (val completed = OperatorExpEmbeddingBuilder().apply(block).complete()) {
        is T -> completed
        else -> error("Attempt to create OperatorExpEmbedding with non-matching number of arguments.")
    }

fun buildUnaryOperator(block: OperatorExpEmbeddingBuilder.() -> Unit) =
    buildOperator<UnaryOperatorExpEmbeddingTemplate>(block)

fun buildBinaryOperator(block: OperatorExpEmbeddingBuilder.() -> Unit) =
    buildOperator<BinaryOperatorExpEmbeddingTemplate>(block)

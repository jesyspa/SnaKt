/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.formver.core.conversion.MethodConversionContext
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.conversion.SubstitutedArgument
import org.jetbrains.kotlin.formver.core.conversion.insertInlineFunctionCall
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding

class LambdaExp(
    val signature: FunctionSignature,
    val function: FirAnonymousFunction,
    private val parentCtx: MethodConversionContext,
    override val labelName: String,
) : CallableEmbedding,
    ExpEmbedding,
    FunctionSignature by signature {
    override val type: TypeEmbedding
        get() = callableType.asTypeEmbedding()

    // A lambda cannot declare default values, so a call to it always supplies every parameter.
    override val takesArgumentPerParameter: Boolean
        get() = false

    override fun insertCall(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding {
        val inlineBody = function.body ?: throw IllegalArgumentException("Lambda $function has a null body.")
        val nonReceiverParamNames = function.valueParameters.map { SubstitutedArgument.ValueParameter(it.symbol) }
        //TODO: can lambdas have dispatch receiver?
        val receiverParamNames =
            if (function.receiverParameter != null) listOf(SubstitutedArgument.ExtensionThis) else emptyList()
        return ctx.insertInlineFunctionCall(
            signature,
            receiverParamNames + nonReceiverParamNames,
            args,
            inlineBody,
            labelName,
            parentCtx,
        )
    }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitLambdaExp(this)
}

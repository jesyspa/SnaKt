/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.formver.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.conversion.MethodConversionContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.SubstitutedArgument
import org.jetbrains.kotlin.formver.conversion.insertInlineFunctionCall
import org.jetbrains.kotlin.formver.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.linearization.LinearizationContext

class LambdaExp(
    val signature: FunctionSignature,
    val function: FirAnonymousFunction,
    private val parentCtx: MethodConversionContext,
    override val labelName: String,
) : CallableEmbedding, StoredResultExpEmbedding,
    FunctionSignature by signature {
    override val type: TypeEmbedding
        get() = callableType.asTypeEmbedding()

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        TODO("create new function object with counter, duplicable (requires toViper restructuring)")
    }

    override fun insertCall(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding {
        val inlineBody = function.body ?: throw IllegalArgumentException("Lambda $function has a null body.")
        val nonReceiverParamNames = function.valueParameters.map { SubstitutedArgument.ValueParameter(it.symbol) }
        //TODO: can lambdas have dispatch receiver?
        val receiverParamNames = if (function.receiverParameter != null) listOf(SubstitutedArgument.ExtensionThis) else emptyList()
        return ctx.insertInlineFunctionCall(
            signature,
            receiverParamNames + nonReceiverParamNames,
            args,
            inlineBody,
            labelName,
            parentCtx,
        )
    }

    override val debugTreeView: TreeView
        get() = PlaintextLeaf("Lambda")

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitLambdaExp(this)
}
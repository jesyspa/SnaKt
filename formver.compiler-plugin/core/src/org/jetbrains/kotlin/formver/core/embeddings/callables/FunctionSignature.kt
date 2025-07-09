/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.AnonymousName
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.core.names.ExtensionReceiverName

interface FunctionSignature {
    val callableType: FunctionTypeEmbedding
    val dispatchReceiver: VariableEmbedding?
    val extensionReceiver: VariableEmbedding?

    val params: List<VariableEmbedding>

    val labelName: String?
        get() = null

    val formalArgs: List<VariableEmbedding>
        get() = listOfNotNull(dispatchReceiver, extensionReceiver) + params
}

abstract class GenericFunctionSignatureMixin : FunctionSignature {
    override val dispatchReceiver: VariableEmbedding?
        get() = callableType.dispatchReceiverType?.let { PlaceholderVariableEmbedding(DispatchReceiverName, it) }

    override val extensionReceiver: VariableEmbedding?
        get() = callableType.extensionReceiverType?.let { PlaceholderVariableEmbedding(ExtensionReceiverName, it) }

    override val params: List<VariableEmbedding>
        get() = callableType.paramTypes.mapIndexed { ix, type -> PlaceholderVariableEmbedding(AnonymousName(ix), type) }
}
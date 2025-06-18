/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.types

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.names.PretypeName
import org.jetbrains.kotlin.formver.viper.mangled

data class FunctionTypeEmbedding(
    val dispatchReceiverType: TypeEmbedding?,
    val extensionReceiverType: TypeEmbedding?,
    val paramTypes: List<TypeEmbedding>,
    val returnType: TypeEmbedding,
    val returnsUnique: Boolean,
) : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.functionType()
    override val name = PretypeName(formalArgTypes.joinToString("$") { it.name.mangled })

    /**
     * The flattened structure of the callable parameters: in case the callable has a receiver
     * it becomes the first argument of the function.
     *
     * `Foo.(Int) -> Int --> (Foo, Int) -> Int`
     */
    val formalArgTypes: List<TypeEmbedding>
        get() = listOfNotNull(dispatchReceiverType, extensionReceiverType) + paramTypes
}

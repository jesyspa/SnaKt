/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.properties

import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.insertCall
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding

class CustomGetter(val getterMethod: CallableEmbedding) : GetterEmbedding {
    override fun getValue(
        receiver: ExpEmbedding,
        ctx: StmtConversionContext,
    ): ExpEmbedding = getterMethod.insertCall(listOf(receiver), ctx, getterMethod.callableType.returnType)
}

class CustomSetter(val setterMethod: CallableEmbedding) : SetterEmbedding {
    override fun setValue(
        receiver: ExpEmbedding,
        value: ExpEmbedding,
        ctx: StmtConversionContext,
    ): ExpEmbedding = setterMethod.insertCall(listOf(receiver, value), ctx, setterMethod.callableType.returnType)
}

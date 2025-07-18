/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.MethodCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.names.PlaceholderReturnVariableName
import org.jetbrains.kotlin.formver.viper.ast.Method

class NonInlineNamedFunction(val signature: FullNamedFunctionSignature) : RichCallableEmbedding,
    FullNamedFunctionSignature by signature {
    override fun insertCall(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding = MethodCall(signature, args)

    override fun toViperMethodHeader(): Method =
        signature.toViperMethod(
            null,
            PlaceholderVariableEmbedding(PlaceholderReturnVariableName, signature.callableType.returnType)
        )
}
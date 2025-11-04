/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings

import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.toViperFunction
import org.jetbrains.kotlin.formver.core.embeddings.callables.toViperMethod
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Function
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Stmt

data class FunctionBodyEmbedding(
    val viperBody: Stmt.Seqn,
    val returnTarget: ReturnTarget,
    val debugExpEmbedding: ExpEmbedding? = null
) {
    fun toViperMethod(signature: FullNamedFunctionSignature): Method =
        signature.toViperMethod(viperBody, returnTarget.variable)
}

data class PureFunctionBodyEmbedding(
    val viperBody: Exp,
    val returnTarget: ReturnTarget,
    val debugExpEmbedding: ExpEmbedding? = null
) {
    fun toViperFunction(signature: FullNamedFunctionSignature): Function =
        signature.toViperFunction(viperBody, returnTarget.variable)
}

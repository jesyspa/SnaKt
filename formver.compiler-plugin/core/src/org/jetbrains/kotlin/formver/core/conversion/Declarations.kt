/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.core.embeddings.expression.Declare
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withType
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding

fun StmtConversionContext.declareLocalProperty(symbol: FirPropertySymbol, initializer: ExpEmbedding?): Declare {
    registerLocalProperty(symbol)
    val variable = embedLocalProperty(symbol)
    return Declare(variable, initializer?.withType(variable.type))
}

fun StmtConversionContext.declareLocalVariable(symbol: FirVariableSymbol<*>, initializer: ExpEmbedding?): Declare {
    registerLocalVariable(symbol)
    val variable = embedLocalVariable(symbol)
    return Declare(variable, initializer?.withType(variable.type))
}

fun StmtConversionContext.declareAnonVar(type: TypeEmbedding, initializer: ExpEmbedding?): Declare {
    val variable = freshAnonVar(type)
    return Declare(variable, initializer?.withType(variable.type))
}

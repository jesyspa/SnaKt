/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*

interface NamedFunctionSignature : FunctionSignature {
    val name: MangledName

    val symbol: FirFunctionSymbol<*>

    override val labelName: String
        get() = symbol.name.asString()
}

fun NamedFunctionSignature.toMethodCall(
    parameters: List<Exp>,
    target: Exp.LocalVar,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) = Stmt.MethodCall(name, parameters, listOf(target), pos, info, trafos)

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.mangled

class Field(
    val name: SymbolicName,
    val type: Type,
    val includeInShortDump: Boolean,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Field> {
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Field =
        viper.silver.ast.Field(name.mangled, type.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
}
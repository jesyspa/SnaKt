/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

sealed interface Declaration : WithSilverMetadata, IntoSilver<viper.silver.ast.Declaration> {
    val name: SymbolicName

    data class LocalVarDecl(
        override val name: SymbolicName,
        val type: Type,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Declaration {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.LocalVarDecl =
            viper.silver.ast.LocalVarDecl(
                name.mangled,
                type.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                silverNoTrafos
            )
    }

    data class LabelDecl(
        override val name: SymbolicName,
        val invariants: List<Exp>,
        override val pos: Position = Position.NoPosition,
        override val info: Info = Info.NoInfo,
    ) : Declaration {
        context(nameResolver: NameResolver)
        override fun toSilver(): viper.silver.ast.Label = viper.silver.ast.Label(
            name.mangled,
            invariants.toSilver().toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            silverNoTrafos
        )
    }
}

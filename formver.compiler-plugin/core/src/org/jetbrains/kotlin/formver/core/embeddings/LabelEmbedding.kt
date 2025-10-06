/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings

import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.formver.viper.ast.Stmt

data class LabelLink(val name: SymbolicName)

data class LabelEmbedding(val name: SymbolicName, val invariants: List<ExpEmbedding> = emptyList())

fun LabelEmbedding.toLink() = LabelLink(name)

fun LabelLink.toViperGoto(ctx: LinearizationContext): Stmt.Goto =
    Label(name, emptyList()).toGoto(pos = ctx.source.asPosition)

fun LabelEmbedding.toViper(ctx: LinearizationContext): Label {
    return Label(name, invariants.pureToViper(toBuiltin = true, ctx.source))
}

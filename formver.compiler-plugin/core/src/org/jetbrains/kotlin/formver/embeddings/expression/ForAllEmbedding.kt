/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.isOf
import org.jetbrains.kotlin.formver.embeddings.asInfo
import org.jetbrains.kotlin.formver.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.types.buildType
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction

class ForAllEmbedding(
    // TODO: support multiple variables
    val variable: VariableEmbedding,
    conditions: List<ExpEmbedding>,
) : OnlyToBuiltinTypeExpEmbedding {
    context(nameResolver: NameResolver)
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
        val conjunction = subexpressions.map { it.toViperBuiltinType(ctx) }.toConjunction()
        return Exp.Forall(
            variables = listOf(variable.toLocalVarDecl()),
            // TODO: right now we hope that Viper will infer triggers successfully, later we might enable user triggers here
            triggers = emptyList(),
            exp =
                if (variable.isOriginallyRef) Exp.Implies(variable.toViper(ctx).isOf(variable.type.runtimeType), conjunction)
                else conjunction,
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo,
        )
    }

    override val subexpressions: List<ExpEmbedding> = conditions

    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun children(): Sequence<ExpEmbedding> = subexpressions.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitForAllEmbedding(this)
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.types.PretypeBuilder
import org.jetbrains.kotlin.formver.embeddings.types.TypeBuilder
import org.jetbrains.kotlin.formver.embeddings.types.buildType
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.formver.viper.ast.Stmt

enum class UnfoldPolicy {
    UNFOLD, UNFOLDING_IN;
}

enum class LogicOperatorPolicy {
    CONVERT_TO_IF, CONVERT_TO_EXPRESSION;
}

/**
 * Context in which an `ExpEmbedding` can be flattened to an `Exp` and a sequence of `Stmt`s.
 *
 * We do not distinguish between expressions and statements on the Kotlin side, but we do on the Viper side.
 * As such, an `ExpEmbedding` can represent a nested structure that has to be flattened into sequences
 * of statements. We call this process linearization.
 */
interface LinearizationContext {
    val source: KtSourceElement?
    val unfoldPolicy: UnfoldPolicy
    val logicOperatorPolicy: LogicOperatorPolicy

    fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding

    fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn
    fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R

    fun addStatement(buildStmt: LinearizationContext.() -> Stmt)
    fun addDeclaration(decl: Declaration)

    fun addModifier(mod: StmtModifier)
}

fun LinearizationContext.freshAnonVar(init: TypeBuilder.() -> PretypeBuilder): AnonymousVariableEmbedding =
    freshAnonVar(buildType(init))

fun LinearizationContext.addLabel(label: Label) {
    addDeclaration(label.toDecl())
    addStatement { label.toStmt() }
}
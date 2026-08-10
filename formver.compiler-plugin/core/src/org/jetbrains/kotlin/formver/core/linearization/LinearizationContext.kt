/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeBuilder
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeBuilder
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.formver.viper.ast.Stmt

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
    // TODO: Move position tracking out of LinearizationContext and into LinearizationVisitor,
    //  passing Position explicitly to ctx methods that need it.
    val source: KtSourceElement?
    val logicOperatorPolicy: LogicOperatorPolicy

    val typeResolver: TypeResolver

    /**
     * Whether an assumption can be recorded here.
     *
     * An `inhale` is a statement, so a context that has to produce a single Viper expression has nowhere to put
     * one. Facts that would have been inhaled are dropped there instead, which costs the verifier information
     * but never adds any.
     */
    val canInhale: Boolean

    fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding

    fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn
    fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R

    fun addStatement(buildStmt: LinearizationContext.() -> Stmt)
    fun addDeclaration(decl: Declaration)
    fun store(lhs: VariableEmbedding, rhs: Linearizable)
    fun addReturn(returnExp: Linearizable, target: ReturnTarget)
    fun addBranch(
        condition: Linearizable,
        thenBranch: Linearizable,
        elseBranch: Linearizable,
        result: VariableEmbedding?
    )

    fun addFieldAccess(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding): Exp

    fun addFieldAccessStoringIn(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding, result: VariableEmbedding)

    fun addModifier(mod: StmtModifier)

    fun resolveVariableName(name: SymbolicName): SymbolicName
}

fun LinearizationContext.freshAnonVar(init: TypeBuilder.() -> PretypeBuilder): AnonymousVariableEmbedding =
    freshAnonVar(buildType(init))

fun LinearizationContext.addLabel(label: Label) {
    addDeclaration(label.toDecl())
    addStatement { label.toStmt() }
}

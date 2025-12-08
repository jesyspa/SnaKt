/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.core.embeddings.toViperGoto
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Standard context for linearization.
 */
data class Linearizer(
    val state: SharedLinearizationState,
    val seqnBuilder: SeqnBuilder,
    override val source: KtSourceElement?,
    val stmtModifierTracker: StmtModifierTracker? = null
) : LinearizationContext {
    override val unfoldPolicy: UnfoldPolicy
        get() = UnfoldPolicy.UNFOLD
    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_IF

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding {
        val variable = state.freshAnonVar(type)
        addDeclaration(variable.toLocalVarDecl())
        return variable
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        val newBuilder = SeqnBuilder(source)
        copy(seqnBuilder = newBuilder).action()
        return newBuilder.block
    }

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        val addStatementContext = object : AddStatementContext {
            override val position: Position = source.asPosition
            override fun addImmediateStatement(statement: Stmt) {
                seqnBuilder.addStatement(statement)
            }
        }
        val newTracker = StmtModifierTracker()
        val stmt = copy(stmtModifierTracker = newTracker).buildStmt()
        newTracker.applyOnEntry(addStatementContext)
        seqnBuilder.addStatement(stmt)
        newTracker.applyOnExit(addStatementContext)
    }

    override fun addDeclaration(decl: Declaration) {
        seqnBuilder.addDeclaration(decl)
    }

    override fun addAssignment(lhs: ExpEmbedding, rhs: ExpEmbedding) {
        val lhsViper = lhs.toViper(this)
        if (lhsViper is Exp.LocalVar) {
            rhs.withType(lhs.type).toViperStoringIn(LinearizationVariableEmbedding(lhsViper.name, lhs.type), this)
        } else {
            val rhsViper = rhs.withType(lhs.type).toViper(this)
            addStatement { Stmt.assign(lhsViper, rhsViper, source.asPosition) }
        }
    }

    override fun addReturn(returnExp: ExpEmbedding, target: ReturnTarget) {
        val retVarViper = target.variable.toViper(this)
        if (retVarViper !is Exp.LocalVar) throw SnaktInternalException(
            source,
            "Translated return variable of function must be a local variable. Got: $retVarViper"
        )
        returnExp.withType(target.variable.type)
            .toViperStoringIn(LinearizationVariableEmbedding(retVarViper.name, returnExp.type), this)
        addStatement { target.label.toLink().toViperGoto(this) }
    }

    override fun addBaseStoredResultExpEmbedding(embedding: BaseStoredResultExpEmbedding): Exp {
        val variable = freshAnonVar(embedding.type)
        embedding.toViperStoringIn(variable, this)
        return variable.toViper(this)
    }

    override fun addModifier(mod: StmtModifier) {
        stmtModifierTracker?.add(mod) ?: error("Not in a statement")
    }
}
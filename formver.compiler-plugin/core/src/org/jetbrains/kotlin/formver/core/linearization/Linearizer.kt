/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.callables.SpecialMethods
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.core.embeddings.toViperGoto
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName
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
    override val typeResolver: TypeResolver,
    val stmtModifierTracker: StmtModifierTracker? = null
) : LinearizationContext {
    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_IF

    override val canInhale: Boolean
        get() = true

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

    override fun store(lhs: VariableEmbedding, rhs: Linearizable) {
        val lhsViper = lhs.toViperExp(this)
        val rhsViper = rhs.toViper(this)
        addStatement { Stmt.assign(lhsViper, rhsViper, source.asPosition) }
    }

    override fun addReturn(returnExp: Linearizable, target: ReturnTarget) {
        returnExp.toViperStoringIn(target.variable, this)
        val returnLabel = target.label ?: throw SnaktInternalException(null, "Return target without label")
        addStatement { returnLabel.toLink().toViperGoto(this) }
    }

    override fun addBranch(
        condition: Linearizable,
        thenBranch: Linearizable,
        elseBranch: Linearizable,
        result: VariableEmbedding?
    ) =
        addStatement {
            val condViper = condition.toViperBuiltinType(this)
            val thenViper = asBlock { thenBranch.toViperMaybeStoringIn(result, this) }
            val elseViper = asBlock { elseBranch.toViperMaybeStoringIn(result, this) }
            Stmt.If(condViper, thenViper, elseViper, source.asPosition)
        }

    override fun addFieldAccess(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding): Exp {
        val result = freshAnonVar(field.type)
        addFieldAccessStoringIn(receiver, receiverType, field, result)
        return result.toViperExp(this)
    }

    override fun addModifier(mod: StmtModifier) {
        stmtModifierTracker?.add(mod) ?: error("Not in a statement")
    }

    override fun addFieldAccessStoringIn(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding, result: VariableEmbedding) {
        addStatement {
            val accessIsManual = with(typeResolver) { (receiverType.pretype as? ClassTypeEmbedding)?.isManual ?: false }
            when (field.accessPolicy) {
                // TODO: Handling a unique field on a shared receiver must be added here.
                AccessPolicy.BY_RECEIVER_UNIQUENESS if !accessIsManual -> {
                    receiver.toViperUnusedResult(this)
                    SpecialMethods.havocMethod.toMethodCall(
                        listOf(field.type.runtimeType),
                        listOf(result.toLocalVarUse())
                    )
                }

                else -> {
                    val receiverViper = receiver.toViper(this)
                    // If the field access is not replaced with havoc,
                    // we might need to unfold some predicate to access it.
                    if (field.unfoldToAccess && !accessIsManual) {
                        unfoldHierarchyPredicates(receiverViper, receiverType, field)
                    }
                    Stmt.assign(
                        result.toLocalVarUse(), Exp.FieldAccess(receiverViper, field.toViper(), source.asPosition)
                    )
                }
            }
        }
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName = name
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.expression.Assert
import org.jetbrains.kotlin.formver.core.embeddings.expression.Assign
import org.jetbrains.kotlin.formver.core.embeddings.expression.BinaryOperatorExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.Block
import org.jetbrains.kotlin.formver.core.embeddings.expression.Cast
import org.jetbrains.kotlin.formver.core.embeddings.expression.Declare
import org.jetbrains.kotlin.formver.core.embeddings.expression.Elvis
import org.jetbrains.kotlin.formver.core.embeddings.expression.EqCmp
import org.jetbrains.kotlin.formver.core.embeddings.expression.ErrorExp
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldAccessPermissions
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldModification
import org.jetbrains.kotlin.formver.core.embeddings.expression.ForAllEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionExp
import org.jetbrains.kotlin.formver.core.embeddings.expression.Goto
import org.jetbrains.kotlin.formver.core.embeddings.expression.GotoChainNode
import org.jetbrains.kotlin.formver.core.embeddings.expression.If
import org.jetbrains.kotlin.formver.core.embeddings.expression.InhaleDirect
import org.jetbrains.kotlin.formver.core.embeddings.expression.InhaleInvariants
import org.jetbrains.kotlin.formver.core.embeddings.expression.InjectionBasedExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.InvokeFunctionObject
import org.jetbrains.kotlin.formver.core.embeddings.expression.Is
import org.jetbrains.kotlin.formver.core.embeddings.expression.LabelExp
import org.jetbrains.kotlin.formver.core.embeddings.expression.LambdaExp
import org.jetbrains.kotlin.formver.core.embeddings.expression.MethodCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.NeCmp
import org.jetbrains.kotlin.formver.core.embeddings.expression.NonDeterministically
import org.jetbrains.kotlin.formver.core.embeddings.expression.Old
import org.jetbrains.kotlin.formver.core.embeddings.expression.PredicateAccessPermissions
import org.jetbrains.kotlin.formver.core.embeddings.expression.PrimitiveFieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.PureExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.SafeCast
import org.jetbrains.kotlin.formver.core.embeddings.expression.SequentialAnd
import org.jetbrains.kotlin.formver.core.embeddings.expression.SequentialOr
import org.jetbrains.kotlin.formver.core.embeddings.expression.Shared
import org.jetbrains.kotlin.formver.core.embeddings.expression.SharingContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.UnaryOperatorExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.UnitLit
import org.jetbrains.kotlin.formver.core.embeddings.expression.While
import org.jetbrains.kotlin.formver.core.embeddings.expression.WithPosition

internal object ExprPurityVisitor : ExpVisitor<Boolean> {

    /* ————— pure nodes ————— */
    override fun visitPureExpEmbedding(e: PureExpEmbedding) = true
    override fun visitUnitLit(e: UnitLit) = true

    /* ————— structural nodes without side effects ————— */
    override fun visitBlock(e: Block) = e.allChildrenPure(this)
    override fun visitBinaryOperatorExpEmbedding(e: BinaryOperatorExpEmbedding) = e.allChildrenPure(this)
    override fun visitSequentialAnd(e: SequentialAnd) = e.allChildrenPure(this)
    override fun visitSequentialOr(e: SequentialOr) = e.allChildrenPure(this)
    override fun visitEqCmp(e: EqCmp) = e.allChildrenPure(this)
    override fun visitNeCmp(e: NeCmp) = e.allChildrenPure(this)
    override fun visitUnaryOperatorExpEmbedding(e: UnaryOperatorExpEmbedding) = e.allChildrenPure(this)
    override fun visitWithPosition(e: WithPosition) = e.allChildrenPure(this)
    override fun visitInjectionBasedExpEmbedding(e: InjectionBasedExpEmbedding) = e.allChildrenPure(this)
    override fun visitSharingContext(e: SharingContext) = e.allChildrenPure(this)

    override fun visitIf(e: If) = false
    override fun visitElvis(e: Elvis) = false
    override fun visitSafeCast(e: SafeCast) = false
    override fun visitCast(e: Cast) = false
    override fun visitIs(e: Is) = false
    override fun visitOld(e: Old) = false
    override fun visitForAllEmbedding(e: ForAllEmbedding) = false


    /* ————— impure nodes ————— */
    override fun visitMethodCall(e: MethodCall) = false   // TODO: Whitelist for annotated methods?
    override fun visitFunctionExp(e: FunctionExp) = false
    override fun visitLambdaExp(e: LambdaExp) = false
    override fun visitInvokeFunctionObject(e: InvokeFunctionObject) = false
    override fun visitShared(e: Shared) = false
    override fun visitDeclare(e: Declare) = false
    override fun visitInhaleDirect(e: InhaleDirect): Boolean = false
    override fun visitErrorExp(e: ErrorExp) = false

    override fun visitAssert(e: Assert): Boolean = false
    override fun visitAssign(e: Assign): Boolean = false
    override fun visitFieldModification(e: FieldModification): Boolean = false
    override fun visitFieldAccess(e: FieldAccess): Boolean = false // TODO
    override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): Boolean = false // TODO
    override fun visitGoto(e: Goto): Boolean = false
    override fun visitGotoChainNode(e: GotoChainNode): Boolean = false
    override fun visitWhile(e: While): Boolean = false
    override fun visitNonDeterministically(e: NonDeterministically): Boolean = false
    override fun visitInhaleInvariants(e: InhaleInvariants): Boolean = false
    override fun visitFieldAccessPermissions(e: FieldAccessPermissions): Boolean = false
    override fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): Boolean = false
    override fun visitLabelExp(e: LabelExp): Boolean = false

    override fun visitDefault(e: ExpEmbedding): Boolean = false
}

private fun ExpEmbedding.allChildrenPure(v: ExprPurityVisitor): Boolean =
    children().all { it.accept(v) }
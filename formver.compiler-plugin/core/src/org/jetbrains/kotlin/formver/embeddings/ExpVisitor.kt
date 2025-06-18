/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.embeddings.expression.Assert
import org.jetbrains.kotlin.formver.embeddings.expression.Assign
import org.jetbrains.kotlin.formver.embeddings.expression.BinaryOperatorExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.Block
import org.jetbrains.kotlin.formver.embeddings.expression.Cast
import org.jetbrains.kotlin.formver.embeddings.expression.Declare
import org.jetbrains.kotlin.formver.embeddings.expression.Elvis
import org.jetbrains.kotlin.formver.embeddings.expression.EqCmp
import org.jetbrains.kotlin.formver.embeddings.expression.ErrorExp
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.embeddings.expression.FieldAccessPermissions
import org.jetbrains.kotlin.formver.embeddings.expression.FieldModification
import org.jetbrains.kotlin.formver.embeddings.expression.ForAllEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FunctionExp
import org.jetbrains.kotlin.formver.embeddings.expression.Goto
import org.jetbrains.kotlin.formver.embeddings.expression.GotoChainNode
import org.jetbrains.kotlin.formver.embeddings.expression.If
import org.jetbrains.kotlin.formver.embeddings.expression.InhaleDirect
import org.jetbrains.kotlin.formver.embeddings.expression.InhaleInvariants
import org.jetbrains.kotlin.formver.embeddings.expression.InjectionBasedExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.InvokeFunctionObject
import org.jetbrains.kotlin.formver.embeddings.expression.Is
import org.jetbrains.kotlin.formver.embeddings.expression.LabelExp
import org.jetbrains.kotlin.formver.embeddings.expression.LambdaExp
import org.jetbrains.kotlin.formver.embeddings.expression.MethodCall
import org.jetbrains.kotlin.formver.embeddings.expression.NeCmp
import org.jetbrains.kotlin.formver.embeddings.expression.NonDeterministically
import org.jetbrains.kotlin.formver.embeddings.expression.Old
import org.jetbrains.kotlin.formver.embeddings.expression.PredicateAccessPermissions
import org.jetbrains.kotlin.formver.embeddings.expression.PrimitiveFieldAccess
import org.jetbrains.kotlin.formver.embeddings.expression.PureExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.SafeCast
import org.jetbrains.kotlin.formver.embeddings.expression.SequentialAnd
import org.jetbrains.kotlin.formver.embeddings.expression.SequentialOr
import org.jetbrains.kotlin.formver.embeddings.expression.Shared
import org.jetbrains.kotlin.formver.embeddings.expression.SharingContext
import org.jetbrains.kotlin.formver.embeddings.expression.UnaryOperatorExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.UnitLit
import org.jetbrains.kotlin.formver.embeddings.expression.While
import org.jetbrains.kotlin.formver.embeddings.expression.WithPosition

interface ExpVisitor<R> {
    fun visitPureExpEmbedding(e: PureExpEmbedding): R
    fun visitBlock(e: Block): R
    fun visitFunctionExp(e: FunctionExp): R
    fun visitGotoChainNode(e: GotoChainNode): R
    fun visitIf(e: If): R
    fun visitElvis(e: Elvis): R
    fun visitLambdaExp(e: LambdaExp): R
    fun visitMethodCall(e: MethodCall): R
    fun visitSafeCast(e: SafeCast): R
    fun visitShared(e: Shared): R
    fun visitAssert(e: Assert): R
    fun visitDeclare(e: Declare): R
    fun visitEqCmp(e: EqCmp): R
    fun visitNeCmp(e: NeCmp): R
    fun visitBinaryOperatorExpEmbedding(e: BinaryOperatorExpEmbedding): R
    fun visitSequentialAnd(e: SequentialAnd): R
    fun visitSequentialOr(e: SequentialOr): R
    fun visitInjectionBasedExpEmbedding(e: InjectionBasedExpEmbedding): R
    fun visitFieldAccessPermissions(e: FieldAccessPermissions): R
    fun visitForAllEmbedding(e: ForAllEmbedding): R
    fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): R
    fun visitCast(e: Cast): R
    fun visitIs(e: Is): R
    fun visitOld(e: Old): R
    fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): R
    fun visitUnaryOperatorExpEmbedding(e: UnaryOperatorExpEmbedding): R
    fun visitErrorExp(e: ErrorExp): R
    fun visitGoto(e: Goto): R
    fun visitInhaleDirect(e: InhaleDirect): R
    fun visitInhaleInvariants(e: InhaleInvariants): R
    fun visitNonDeterministically(e: NonDeterministically): R
    fun visitWhile(e: While): R
    fun visitFieldAccess(e: FieldAccess): R
    fun visitInvokeFunctionObject(e: InvokeFunctionObject): R
    fun visitAssign(e: Assign): R
    fun visitFieldModification(e: FieldModification): R
    fun visitLabelExp(e: LabelExp): R
    fun visitUnitLit(e: UnitLit): R
    fun visitSharingContext(e: SharingContext): R
    fun visitWithPosition(e: WithPosition): R
    fun visitDefault(e: ExpEmbedding): R
}
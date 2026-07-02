/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings

import org.jetbrains.kotlin.formver.core.embeddings.expression.*

interface ExpVisitor<R> {
    fun visitDefault(e: ExpEmbedding): R
    fun visitBlock(e: Block): R = visitDefault(e)
    fun visitFunctionExp(e: FunctionExp): R = visitDefault(e)
    fun visitGotoChainNode(e: GotoChainNode): R = visitDefault(e)
    fun visitIf(e: If): R = visitDefault(e)
    fun visitElvis(e: Elvis): R = visitDefault(e)
    fun visitReturn(e: Return): R = visitDefault(e)
    fun visitLambdaExp(e: LambdaExp): R = visitDefault(e)
    fun visitMethodCall(e: MethodCall): R = visitDefault(e)
    fun visitFunctionCall(e: FunctionCall): R = visitDefault(e)
    fun visitSafeCast(e: SafeCast): R = visitDefault(e)
    fun visitShared(e: Shared): R = visitDefault(e)
    fun visitAssert(e: Assert): R = visitDefault(e)
    fun visitDeclare(e: Declare): R = visitDefault(e)
    fun visitEqCmp(e: EqCmp): R = visitDefault(e)
    fun visitNeCmp(e: NeCmp): R = visitDefault(e)
    fun visitBinaryOperatorExpEmbedding(e: BinaryOperatorExpEmbedding): R = visitDefault(e)
    fun visitSequentialAnd(e: SequentialAnd): R = visitDefault(e)
    fun visitSequentialOr(e: SequentialOr): R = visitDefault(e)
    fun visitInjectionBasedExpEmbedding(e: InjectionBasedExpEmbedding): R = visitDefault(e)
    fun visitFieldAccessPermissions(e: FieldAccessPermissions): R = visitDefault(e)
    fun visitForAllEmbedding(e: ForAllEmbedding): R = visitDefault(e)
    fun visitExistsEmbedding(e: ExistsEmbedding): R = visitDefault(e)
    fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): R = visitDefault(e)
    fun visitCast(e: Cast): R = visitDefault(e)
    fun visitIs(e: Is): R = visitDefault(e)
    fun visitOld(e: Old): R = visitDefault(e)
    fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): R = visitDefault(e)
    fun visitUnaryOperatorExpEmbedding(e: UnaryOperatorExpEmbedding): R = visitDefault(e)
    fun visitErrorExp(e: ErrorExp): R = visitDefault(e)
    fun visitGoto(e: Goto): R = visitDefault(e)
    fun visitInhaleDirect(e: InhaleDirect): R = visitDefault(e)
    fun visitInhaleInvariants(e: InhaleInvariants): R = visitDefault(e)
    fun visitNonDeterministically(e: NonDeterministically): R = visitDefault(e)
    fun visitWhile(e: While): R = visitDefault(e)
    fun visitFieldAccess(e: FieldAccess): R = visitDefault(e)
    fun visitInvokeFunctionObject(e: InvokeFunctionObject): R = visitDefault(e)
    fun visitAssign(e: Assign): R = visitDefault(e)
    fun visitFieldModification(e: FieldModification): R = visitDefault(e)
    fun visitLabelExp(e: LabelExp): R = visitDefault(e)
    fun visitUnitLit(e: UnitLit): R = visitDefault(e)
    fun visitSharingContext(e: SharingContext): R = visitDefault(e)
    fun visitWithPosition(e: WithPosition): R = visitDefault(e)
    fun visitLiteralEmbedding(e: LiteralEmbedding): R = visitDefault(e)
    fun visitExpWrapper(e: ExpWrapper): R = visitDefault(e)
    fun visitVariableEmbedding(e: VariableEmbedding): R = visitDefault(e)
    fun visitAccEmbedding(e: AccEmbedding): R = visitDefault(e)
}

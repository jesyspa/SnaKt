/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings

import org.jetbrains.kotlin.formver.core.embeddings.expression.*

interface ExpVisitor<R> {
    fun visitBlock(e: Block): R
    fun visitFunctionExp(e: FunctionExp): R
    fun visitGotoChainNode(e: GotoChainNode): R
    fun visitIf(e: If): R
    fun visitElvis(e: Elvis): R
    fun visitReturn(e: Return): R
    fun visitLambdaExp(e: LambdaExp): R
    fun visitMethodCall(e: MethodCall): R
    fun visitFunctionCall(e: FunctionCall): R
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
    fun visitLiteralEmbedding(e: LiteralEmbedding): R
    fun visitExpWrapper(e: ExpWrapper): R
    fun visitVariableEmbedding(e: VariableEmbedding): R
    fun visitAccEmbedding(e: AccEmbedding): R
}
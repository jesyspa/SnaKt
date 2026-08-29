/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings

import org.jetbrains.kotlin.formver.core.embeddings.expression.*

/**
 * Visitor over the concrete subclasses of [ExpEmbedding].
 *
 * Every concrete [ExpEmbedding] subclass has a corresponding `visitXxx` method here, and all of
 * them are abstract. This means that implementing this interface directly requires overriding
 * every single one of them: forgetting to handle a newly added [ExpEmbedding] subclass is a
 * compile error, not a runtime failure.
 *
 * Visitors that want a generic fallback for nodes without a bespoke override (e.g. for debug
 * rendering) should implement [DefaultingExpVisitor] instead.
 */
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
    fun visitIdentityCmp(e: IdentityCmp): R
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
    fun visitLiteralEmbedding(e: LiteralEmbedding): R
    fun visitExpWrapper(e: ExpWrapper): R
    fun visitVariableEmbedding(e: VariableEmbedding): R
    fun visitAccEmbedding(e: AccEmbedding): R
    fun visitPermissionLit(e: PermissionLit): R
    fun visitFold(e: Fold): R
    fun visitUnfold(e: Unfold): R
    fun visitAllocateObject(e: AllocateObject): R
    fun visitInitField(e: InitField): R
}

/**
 * An [ExpVisitor] that falls back to [visitDefault] for any concrete [ExpEmbedding] subclass
 * without a bespoke `visitXxx` override.
 *
 * Implement this instead of [ExpVisitor] when a generic fallback is a legitimate design choice
 * (e.g. debug rendering), rather than an omission.
 */
interface DefaultingExpVisitor<R> : ExpVisitor<R> {
    fun visitDefault(e: ExpEmbedding): R

    override fun visitBlock(e: Block): R = visitDefault(e)
    override fun visitFunctionExp(e: FunctionExp): R = visitDefault(e)
    override fun visitGotoChainNode(e: GotoChainNode): R = visitDefault(e)
    override fun visitIf(e: If): R = visitDefault(e)
    override fun visitElvis(e: Elvis): R = visitDefault(e)
    override fun visitReturn(e: Return): R = visitDefault(e)
    override fun visitLambdaExp(e: LambdaExp): R = visitDefault(e)
    override fun visitMethodCall(e: MethodCall): R = visitDefault(e)
    override fun visitFunctionCall(e: FunctionCall): R = visitDefault(e)
    override fun visitSafeCast(e: SafeCast): R = visitDefault(e)
    override fun visitShared(e: Shared): R = visitDefault(e)
    override fun visitAssert(e: Assert): R = visitDefault(e)
    override fun visitDeclare(e: Declare): R = visitDefault(e)
    override fun visitEqCmp(e: EqCmp): R = visitDefault(e)
    override fun visitNeCmp(e: NeCmp): R = visitDefault(e)
    override fun visitIdentityCmp(e: IdentityCmp): R = visitDefault(e)
    override fun visitBinaryOperatorExpEmbedding(e: BinaryOperatorExpEmbedding): R = visitDefault(e)
    override fun visitSequentialAnd(e: SequentialAnd): R = visitDefault(e)
    override fun visitSequentialOr(e: SequentialOr): R = visitDefault(e)
    override fun visitInjectionBasedExpEmbedding(e: InjectionBasedExpEmbedding): R = visitDefault(e)
    override fun visitFieldAccessPermissions(e: FieldAccessPermissions): R = visitDefault(e)
    override fun visitForAllEmbedding(e: ForAllEmbedding): R = visitDefault(e)
    override fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): R = visitDefault(e)
    override fun visitCast(e: Cast): R = visitDefault(e)
    override fun visitIs(e: Is): R = visitDefault(e)
    override fun visitOld(e: Old): R = visitDefault(e)
    override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): R = visitDefault(e)
    override fun visitUnaryOperatorExpEmbedding(e: UnaryOperatorExpEmbedding): R = visitDefault(e)
    override fun visitErrorExp(e: ErrorExp): R = visitDefault(e)
    override fun visitGoto(e: Goto): R = visitDefault(e)
    override fun visitInhaleDirect(e: InhaleDirect): R = visitDefault(e)
    override fun visitInhaleInvariants(e: InhaleInvariants): R = visitDefault(e)
    override fun visitNonDeterministically(e: NonDeterministically): R = visitDefault(e)
    override fun visitWhile(e: While): R = visitDefault(e)
    override fun visitFieldAccess(e: FieldAccess): R = visitDefault(e)
    override fun visitInvokeFunctionObject(e: InvokeFunctionObject): R = visitDefault(e)
    override fun visitAssign(e: Assign): R = visitDefault(e)
    override fun visitFieldModification(e: FieldModification): R = visitDefault(e)
    override fun visitLabelExp(e: LabelExp): R = visitDefault(e)
    override fun visitUnitLit(e: UnitLit): R = visitDefault(e)
    override fun visitSharingContext(e: SharingContext): R = visitDefault(e)
    override fun visitWithPosition(e: WithPosition): R = visitDefault(e)
    override fun visitLiteralEmbedding(e: LiteralEmbedding): R = visitDefault(e)
    override fun visitExpWrapper(e: ExpWrapper): R = visitDefault(e)
    override fun visitVariableEmbedding(e: VariableEmbedding): R = visitDefault(e)
    override fun visitAccEmbedding(e: AccEmbedding): R = visitDefault(e)
    override fun visitPermissionLit(e: PermissionLit): R = visitDefault(e)
    override fun visitFold(e: Fold): R = visitDefault(e)
    override fun visitUnfold(e: Unfold): R = visitDefault(e)
    override fun visitAllocateObject(e: AllocateObject): R = visitDefault(e)
    override fun visitInitField(e: InitField): R = visitDefault(e)
}

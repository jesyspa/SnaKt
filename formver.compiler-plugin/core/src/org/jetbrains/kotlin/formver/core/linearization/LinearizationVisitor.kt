/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain.Companion.isOf
import org.jetbrains.kotlin.formver.core.embeddings.*
import org.jetbrains.kotlin.formver.core.embeddings.callables.toFuncApp
import org.jetbrains.kotlin.formver.core.embeddings.callables.toMethodCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.fillHoles
import org.jetbrains.kotlin.formver.core.embeddings.types.injection
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.ast.viperLiteral

data class LinearizationVisitor(
    val source: KtSourceElement? = null,
) : ExpVisitor<Linearizable> {

    /**
     * Linearize a child expression, propagating visitor state (including position).
     */
    private fun ExpEmbedding.linearize(): Linearizable = accept(this@LinearizationVisitor)

    // region Control Flow

    override fun visitBlock(e: Block): Linearizable = object : OptionalResultLinearizable(e) {
        override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
            if (e.exps.isEmpty()) return
            for (exp in e.exps.take(e.exps.size - 1)) {
                exp.linearize().toViperUnusedResult(ctx)
            }
            e.exps.last().linearize().toViperMaybeStoringIn(result, ctx)
        }
    }

    override fun visitIf(e: If): Linearizable = object : OptionalResultLinearizable(e) {
        override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
            ctx.addBranch(e.condition.linearize(), e.thenBranch.linearize(), e.elseBranch.linearize(), result)
        }
    }

    override fun visitWhile(e: While): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addLabel(e.continueLabel.toViper(ctx))
            val condVar = ctx.freshAnonVar { boolean() }
            e.condition.linearize().toViperStoringIn(condVar, ctx)
            ctx.addStatement {
                val bodyBlock = ctx.asBlock {
                    e.body.linearize().toViperUnusedResult(this)
                    addStatement { e.continueLabel.toLink().toViperGoto(this) }
                }
                Stmt.If(condVar.linearize().toViperBuiltinType(ctx), bodyBlock, els = Stmt.Seqn(), ctx.source.asPosition)
            }
            ctx.addLabel(e.breakLabel.toViper(ctx))

            e.invariants.forEach {
                ctx.addStatement {
                    Stmt.Assert(it.pureToViper(toBuiltin = true, ctx.typeResolver))
                }
            }
        }
    }

    override fun visitGoto(e: Goto): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addStatement { e.target.toViperGoto(ctx) }
        }
    }

    override fun visitLabelExp(e: LabelExp): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addLabel(e.label.toViper(ctx))
        }
    }

    override fun visitGotoChainNode(e: GotoChainNode): Linearizable = object : OptionalResultLinearizable(e) {
        override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
            e.label?.let { ctx.addLabel(it.toViper(ctx)) }
            ctx.addStatement {
                e.exp.linearize().toViperMaybeStoringIn(result, ctx)
                e.next.toViperGoto(ctx)
            }
        }
    }

    override fun visitNonDeterministically(e: NonDeterministically): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addStatement {
                val choice = ctx.freshAnonVar { boolean() }
                val expViper = ctx.asBlock { e.exp.linearize().toViper(this) }
                Stmt.If(choice.linearize().toViperBuiltinType(ctx), expViper, Stmt.Seqn(), ctx.source.asPosition)
            }
        }
    }

    override fun visitMethodCall(e: MethodCall): Linearizable = object : StoredResultLinearizable(e) {
        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            ctx.addStatement {
                e.method.toMethodCall(
                    e.args.map { it.linearize().toViper(ctx) },
                    result.toLocalVarUse(ctx.source.asPosition),
                    ctx.source.asPosition
                )
            }
        }
    }

    override fun visitFunctionCall(e: FunctionCall): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp = e.function.toFuncApp(
            e.args.map { it.linearize().toViper(ctx) },
            ctx.source.asPosition
        )
    }

    override fun visitInvokeFunctionObject(e: InvokeFunctionObject): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp {
            val variable = ctx.freshAnonVar(e.type)
            e.receiver.linearize().toViperUnusedResult(ctx)
            for (arg in e.args) arg.linearize().toViperUnusedResult(ctx)
            return variable.withInvariants(ctx.typeResolver) {
                proven = true
                access = true
            }.linearize().toViper(ctx)
        }

        // Must call toViper (not just iterate children) to emit the invariant inhales.
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            toViper(ctx)
        }
    }

    override fun visitFunctionExp(e: FunctionExp): Linearizable = object : OptionalResultLinearizable(e) {
        override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
            e.body.linearize().toViperMaybeStoringIn(result, ctx)
            ctx.addLabel(e.returnLabel.toViper(ctx))
        }
    }

    override fun visitElvis(e: Elvis): Linearizable = object : StoredResultLinearizable(e) {
        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            val leftViper = e.left.linearize().toViper(ctx)
            val leftWrapped = ExpWrapper(leftViper, e.left.type)
            val conditional = If(leftWrapped.notNullCmp(), leftWrapped.withType(e.type), e.right.withType(e.type), e.type)
            conditional.linearize().toViperStoringIn(result, ctx)
        }
    }

    override fun visitReturn(e: Return): Linearizable = object : OptionalResultLinearizable(e) {
        override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
            ctx.addReturn(e.returnExp.linearize(), e.target)
        }
    }

    // endregion

    // region Literals

    override fun visitLiteralEmbedding(e: LiteralEmbedding): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp =
            if (e is NullLit) RuntimeTypeDomain.nullValue(pos = ctx.source.asPosition)
            else e.type.injection.toRef(
                e.value.viperLiteral(ctx.source.asPosition, e.sourceRole.asInfo),
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo,
            )
    }

    override fun visitUnitLit(e: UnitLit): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) = Unit
    }

    // endregion

    // region Variables

    override fun visitVariableEmbedding(e: VariableEmbedding): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp = e.toViperExp(ctx)
    }

    // endregion

    // region Special

    override fun visitExpWrapper(e: ExpWrapper): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp = e.value
    }

    override fun visitErrorExp(e: ErrorExp): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addStatement { Stmt.Inhale(Exp.BoolLit(false, ctx.source.asPosition), ctx.source.asPosition) }
        }
    }

    override fun visitAssert(e: Assert): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addStatement { Stmt.Assert(e.exp.linearize().toViperBuiltinType(ctx), ctx.source.asPosition) }
        }
    }

    override fun visitInhaleDirect(e: InhaleDirect): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addStatement { Stmt.Inhale(e.exp.linearize().toViperBuiltinType(ctx), ctx.source.asPosition) }
        }
    }

    // endregion

    // region Type Operations

    override fun visitIs(e: Is): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp =
            RuntimeTypeDomain.boolInjection.toRef(
                RuntimeTypeDomain.isSubtype(
                    RuntimeTypeDomain.typeOf(e.inner.linearize().toViper(ctx), pos = ctx.source.asPosition),
                    e.comparisonType.runtimeType,
                    pos = ctx.source.asPosition,
                    info = e.sourceRole.asInfo
                ),
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo
            )
    }

    override fun visitCast(e: Cast): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp = e.inner.linearize().toViper(ctx)
    }

    override fun visitSafeCast(e: SafeCast): Linearizable = object : StoredResultLinearizable(e) {
        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            val expViper = e.exp.linearize().toViper(ctx)
            val expWrapped = ExpWrapper(expViper, e.exp.type)
            val conditional = If(Is(expWrapped, e.targetType), expWrapped.withType(e.type), NullLit.withType(e.type), e.type)
            conditional.linearize().toViperStoringIn(result, ctx)
        }
    }

    override fun visitInhaleInvariants(e: InhaleInvariants): Linearizable =
        InhalingIfPossible(inhaling = linearizeInhaleInvariants(e), plain = e.exp.linearize())

    private fun linearizeInhaleInvariants(e: InhaleInvariants): Linearizable {
        // InhaleInvariantsForVariable: expression is a variable, use OnlyToViper-style
        // (toViperUnusedResult must call toViper, not just iterate children, to emit the inhales)
        if (e.exp.underlyingVariable != null) {
            return object : DirectResultLinearizable(e, this@LinearizationVisitor) {
                override fun toViper(ctx: LinearizationContext): Exp {
                    val variable = e.exp.underlyingVariable ?: error("Use of InhaleInvariantsForVariable for non-variable")
                    for (invariant in e.invariants.fillHoles(variable)) {
                        ctx.addStatement {
                            Stmt.Inhale(
                                invariant.pureToViper(
                                    toBuiltin = true,
                                    ctx.typeResolver,
                                    ctx.source
                                ), ctx.source.asPosition
                            )
                        }
                    }
                    return e.exp.linearize().toViper(ctx)
                }

                override fun toViperUnusedResult(ctx: LinearizationContext) {
                    toViper(ctx)
                }
            }
        }
        // InhaleInvariantsForExp: store result then inhale invariants
        return object : StoredResultLinearizable(e) {
            override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
                e.exp.linearize().toViperStoringIn(result, ctx)
                for (invariant in e.invariants.fillHoles(result)) {
                    ctx.addStatement {
                        Stmt.Inhale(
                            invariant.pureToViper(
                                toBuiltin = true,
                                ctx.typeResolver,
                                ctx.source
                            ), ctx.source.asPosition
                        )
                    }
                }
            }
        }
    }

    // endregion

    // region Operators

    override fun visitBinaryOperatorExpEmbedding(e: BinaryOperatorExpEmbedding): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp =
            e.refsOperation(
                e.left.linearize().toViper(ctx),
                e.right.linearize().toViper(ctx),
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo
            )

        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            e.builtinsOperation(
                e.left.linearize().toViperBuiltinType(ctx),
                e.right.linearize().toViperBuiltinType(ctx),
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo
            )
    }

    override fun visitUnaryOperatorExpEmbedding(e: UnaryOperatorExpEmbedding): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp =
            e.refsOperation(e.inner.linearize().toViper(ctx), pos = ctx.source.asPosition, info = e.sourceRole.asInfo)

        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            e.builtinsOperation(e.inner.linearize().toViperBuiltinType(ctx), pos = ctx.source.asPosition, info = e.sourceRole.asInfo)
    }

    override fun visitInjectionBasedExpEmbedding(e: InjectionBasedExpEmbedding): Linearizable =
        error("visitInjectionBasedExpEmbedding should not be called directly")

    override fun visitSequentialAnd(e: SequentialAnd): Linearizable = sequentialLogicOperator(e)
    override fun visitSequentialOr(e: SequentialOr): Linearizable = sequentialLogicOperator(e)

    private fun sequentialLogicOperator(e: SequentialLogicOperatorEmbedding): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        private fun replacement(ctx: LinearizationContext) = e.operatorReplacement(ctx)

        override fun toViper(ctx: LinearizationContext): Exp =
            replacement(ctx).linearize().toViper(ctx)

        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            replacement(ctx).linearize().toViperBuiltinType(ctx)

        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            replacement(ctx).linearize().toViperStoringIn(result, ctx)
        }
    }

    // endregion

    // region Comparisons

    override fun visitEqCmp(e: EqCmp): Linearizable = comparisonLinearizable(e)
    override fun visitNeCmp(e: NeCmp): Linearizable = comparisonLinearizable(e)

    override fun visitIdentityCmp(e: IdentityCmp): Linearizable = comparisonLinearizable(e)

    private fun comparisonLinearizable(e: AnyComparisonExpression): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp =
            RuntimeTypeDomain.boolInjection.toRef(
                toViperBuiltinType(ctx),
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo
            )

        override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
            fun ExpEmbedding.operand(): Exp =
                if (e.comparesUnwrapped) linearize().toViperBuiltinType(ctx) else linearize().toViper(ctx)
            return e.comparisonOperation(
                e.left.operand(),
                e.right.operand(),
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo
            )
        }
    }

    // endregion

    // region Invariant

    override fun visitOld(e: Old): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp =
            Exp.Old(e.inner.linearize().toViper(ctx), ctx.source.asPosition)

        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            Exp.Old(e.inner.linearize().toViperBuiltinType(ctx), ctx.source.asPosition)
    }

    // endregion

    // region Field Access

    override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): Linearizable = object : DirectResultLinearizable(e, this@LinearizationVisitor) {
        override fun toViper(ctx: LinearizationContext): Exp =
            Exp.FieldAccess(e.inner.linearize().toViper(ctx), e.field.toViper(), ctx.source.asPosition)
    }

    override fun visitFieldAccess(e: FieldAccess): Linearizable = object : Linearizable {
        private val receiverLinearizable = e.receiver.linearize()

        override fun toViper(ctx: LinearizationContext): Exp {
            if (e.field.accessPolicy == AccessPolicy.ALWAYS_WRITEABLE) {
                return PrimitiveFieldAccess(e.receiver, e.field).linearize().toViper(ctx)
            }
            return ctx.addFieldAccess(receiverLinearizable, e.receiver.type, e.field)
        }

        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            ctx.addFieldAccessStoringIn(receiverLinearizable, e.receiver.type, e.field, result)
        }

        override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
            if (result != null) toViperStoringIn(result, ctx)
            else toViperUnusedResult(ctx)
        }

        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            defaultToViperBuiltinType(::toViper, e.type, e.sourceRole, ctx)

        override fun toViperUnusedResult(ctx: LinearizationContext) {
            receiverLinearizable.toViperUnusedResult(ctx)
        }
    }

    override fun visitFieldModification(e: FieldModification): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            val accessIsManual = with(ctx.typeResolver) { (e.receiver.type.pretype as? ClassTypeEmbedding)?.isManual ?: false }
            when (e.field.accessPolicy) {
                AccessPolicy.BY_RECEIVER_UNIQUENESS if !accessIsManual -> {
                    e.receiver.linearize().toViperUnusedResult(ctx)
                    e.newValue.linearize().toViperUnusedResult(ctx)
                }
                else -> {
                    val receiverViper = e.receiver.linearize().toViper(ctx)
                    if (e.field.unfoldToAccess && !accessIsManual) {
                        ctx.unfoldHierarchyPredicates(receiverViper, e.receiver.type, e.field)
                    }
                    val newValueViper = e.newValue.linearize().toViper(ctx)
                    ctx.addStatement {
                        Stmt.FieldAssign(
                            Exp.FieldAccess(receiverViper, e.field.toViper()),
                            newValueViper,
                            ctx.source.asPosition
                        )
                    }
                }
            }
        }
    }

    override fun visitFieldAccessPermissions(e: FieldAccessPermissions): Linearizable = object : OnlyToBuiltinLinearizable(e, this@LinearizationVisitor) {
        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            e.inner.linearize().toViper(ctx).fieldAccessPredicate(e.field.toViper(), e.perm, ctx.source.asPosition)
    }

    override fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): Linearizable = object : OnlyToBuiltinLinearizable(e, this@LinearizationVisitor) {
        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            Exp.PredicateAccess(e.predicateName, e.args.map { it.linearize().toViper(ctx) }, e.perm, ctx.source.asPosition)
    }

    override fun visitUnfold(e: Unfold): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addStatement {
                Stmt.Unfold(e.pred.linearize().toViperBuiltinType(ctx) as Exp.PredicateAccess)
            }
        }
    }

    override fun visitFold(e: Fold): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addStatement {
                Stmt.Fold(e.pred.linearize().toViperBuiltinType(ctx) as Exp.PredicateAccess)
            }
        }
    }

    // endregion

    // region Assignment / Declaration

    override fun visitAssign(e: Assign): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            e.rhs.linearize().toViperStoringIn(LinearizationVariableEmbedding(e.lhs.name, e.lhs.type), ctx)
        }
    }

    override fun visitDeclare(e: Declare): Linearizable = object : UnitResultLinearizable(e) {
        override fun toViperUnusedResult(ctx: LinearizationContext) {
            ctx.addDeclaration(e.variable.toLocalVarDecl(ctx.source.asPosition))
            e.initializer
                ?.linearize()?.toViperStoringIn(LinearizationVariableEmbedding(e.variable.name, e.variable.type), ctx)
        }
    }

    // endregion

    // region ForAll / Acc

    override fun visitForAllEmbedding(e: ForAllEmbedding): Linearizable = object : OnlyToBuiltinLinearizable(e, this@LinearizationVisitor) {
        override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
            val conjunction = e.conditions.pureToViper(true, ctx.typeResolver, ctx.source).toConjunction()
            val viperTriggers = e.triggerExpressions.map { triggerExpr ->
                Exp.Trigger(listOf(triggerExpr.linearize().toViperBuiltinType(ctx)))
            }
            return Exp.Forall(
                variables = listOf(e.variable.toLocalVarDecl()),
                triggers = viperTriggers,
                exp = if (e.variable.isOriginallyRef) Exp.Implies(
                    e.variable.toViperExp(ctx).isOf(e.variable.type.runtimeType),
                    conjunction
                )
                else conjunction,
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo,
            )
        }
    }

    override fun visitAccEmbedding(e: AccEmbedding): Linearizable = object : OnlyToBuiltinLinearizable(e, this@LinearizationVisitor) {
        override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
            val field = Exp.FieldAccess(
                e.receiver.linearize().toViper(ctx),
                e.field.toViper(),
                ctx.source.asPosition,
            )
            return Exp.Acc(
                field = field,
                perm = e.perm,
                pos = ctx.source.asPosition,
                info = e.sourceRole.asInfo,
            )
        }
    }

    override fun visitPermissionLit(e: PermissionLit): Linearizable =
        error("PermissionLit should not be linearized; it is consumed directly by `acc` argument handling")

    // endregion

    // region Meta / Sharing

    override fun visitWithPosition(e: WithPosition): Linearizable {
        val innerLinearizable = e.inner.accept(copy(source = e.source))
        return object : Linearizable {
            override fun toViper(ctx: LinearizationContext): Exp =
                ctx.withPosition(e.source) { innerLinearizable.toViper(this) }

            override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
                ctx.withPosition(e.source) { innerLinearizable.toViperStoringIn(result, this) }
            }

            override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
                ctx.withPosition(e.source) { innerLinearizable.toViperMaybeStoringIn(result, this) }
            }

            override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
                ctx.withPosition(e.source) { innerLinearizable.toViperBuiltinType(this) }

            override fun toViperUnusedResult(ctx: LinearizationContext) {
                ctx.withPosition(e.source) { innerLinearizable.toViperUnusedResult(this) }
            }
        }
    }

    override fun visitSharingContext(e: SharingContext): Linearizable = object : Linearizable {
        override fun toViper(ctx: LinearizationContext): Exp =
            e.inner.linearize().toViper(ctx).also { e.sharedExp = null }

        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            e.inner.linearize().toViperStoringIn(result, ctx)
            e.sharedExp = null
        }

        override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
            e.inner.linearize().toViperMaybeStoringIn(result, ctx)
            e.sharedExp = null
        }

        override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
            e.inner.linearize().toViperBuiltinType(ctx).also { e.sharedExp = null }

        override fun toViperUnusedResult(ctx: LinearizationContext) {
            e.inner.linearize().toViperUnusedResult(ctx)
            e.sharedExp = null
        }
    }

    override fun visitShared(e: Shared): Linearizable = object : StoredResultLinearizable(e) {
        override fun toViper(ctx: LinearizationContext): Exp =
            e.context.tryInitShared { e.inner.linearize().toViper(ctx) }

        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            e.context.tryInitShared { e.inner.linearize().toViperStoringIn(result, ctx); result.toViperExp(ctx) }
        }

        override fun toViperUnusedResult(ctx: LinearizationContext) {
            e.context.tryInitShared {
                e.inner.linearize().toViperUnusedResult(ctx); UnitLit.pureToViper(toBuiltin = false, ctx.typeResolver)
            }
        }
    }

    // endregion

    // region Lambda

    override fun visitLambdaExp(e: LambdaExp): Linearizable = object : StoredResultLinearizable(e) {
        override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
            TODO("create new function object with counter, duplicable (requires toViper restructuring)")
        }
    }

    // endregion
}

/**
 * Emits the invariants where the context can hold an `inhale`, and the bare expression where it cannot.
 *
 * The choice depends on the context rather than on the embedding, so it can only be made once the
 * [LinearizationContext] is in hand.
 */
private class InhalingIfPossible(private val inhaling: Linearizable, private val plain: Linearizable) : Linearizable {
    private fun LinearizationContext.pick(): Linearizable = if (canInhale) inhaling else plain

    override fun toViper(ctx: LinearizationContext): Exp = ctx.pick().toViper(ctx)

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) =
        ctx.pick().toViperStoringIn(result, ctx)

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) =
        ctx.pick().toViperMaybeStoringIn(result, ctx)

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp = ctx.pick().toViperBuiltinType(ctx)

    override fun toViperUnusedResult(ctx: LinearizationContext) = ctx.pick().toViperUnusedResult(ctx)
}

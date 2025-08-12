/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.toMethodCall
import org.jetbrains.kotlin.formver.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.types.buildType
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.linearization.addLabel
import org.jetbrains.kotlin.formver.linearization.freshAnonVar
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

private data class BlockImpl(override val exps: List<ExpEmbedding>) : Block

fun blockOf(vararg exps: ExpEmbedding): Block = BlockImpl(exps.toList())

fun List<ExpEmbedding>.toBlock(): Block = BlockImpl(this)

fun Block(actions: MutableList<ExpEmbedding>.() -> Unit): Block = BlockImpl(buildList {
    actions()
})

sealed interface Block : OptionalResultExpEmbedding {
    val exps: List<ExpEmbedding>
    override val type: TypeEmbedding
        get() = exps.lastOrNull()?.type ?: buildType { unit() }
    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (exps.isEmpty()) return

        for (exp in exps.take(exps.size - 1)) {
            exp.toViperUnusedResult(ctx)
        }
        exps.last().toViperMaybeStoringIn(result, ctx)
    }
    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = BlockNode(exps.map { it.debugTreeView })

    override fun children(): Sequence<ExpEmbedding> = exps.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitBlock(this)
}


data class If(val condition: ExpEmbedding, val thenBranch: ExpEmbedding, val elseBranch: ExpEmbedding, override val type: TypeEmbedding) :
    OptionalResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        ctx.addStatement {
            val condViper = condition.toViperBuiltinType(ctx)
            val thenViper = ctx.asBlock { thenBranch.withType(type).toViperMaybeStoringIn(result, this) }
            val elseViper = ctx.asBlock { elseBranch.withType(type).toViperMaybeStoringIn(result, this) }
            Stmt.If(condViper, thenViper, elseViper, ctx.source.asPosition)
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(condition, thenBranch, elseBranch)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(condition, thenBranch, elseBranch)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIf(this)
}

data class While(
    val condition: ExpEmbedding,
    val body: ExpEmbedding,
    val breakLabelName: MangledName,
    val continueLabelName: MangledName,
    val invariants: List<ExpEmbedding>,
) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { unit() }

    val continueLabel = LabelEmbedding(continueLabelName, invariants)
    val breakLabel = LabelEmbedding(breakLabelName)
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addLabel(continueLabel.toViper(ctx))
        val condVar = ctx.freshAnonVar { boolean() }
        condition.toViperStoringIn(condVar, ctx)
        ctx.addStatement {
            val bodyBlock = ctx.asBlock {
                body.toViperUnusedResult(this)
                addStatement { continueLabel.toLink().toViperGoto(this) }
            }
            Stmt.If(condVar.toViperBuiltinType(ctx), bodyBlock, els = Stmt.Seqn(), ctx.source.asPosition)
        }
        ctx.addLabel(breakLabel.toViper(ctx))

        // TODO: this logic can be rewritten back to invariants once the version of Viper is updated
        invariants.forEach {
            ctx.addStatement {
                Stmt.Assert(it.pureToViper(toBuiltin = true))
            }
        }
    }

    // TODO: add invariants
    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(condition, body)

    context(nameResolver: NameResolver)
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(
            breakLabel.debugTreeView.withDesignation("break"),
            continueLabel.debugTreeView.withDesignation("continue"),
        )

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(condition, body)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitWhile(this)
}

data class Goto(val target: LabelLink) : NoResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { nothing() }
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        ctx.addStatement { target.toViperGoto(ctx) }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()
    context(nameResolver: NameResolver)
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(target.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitGoto(this)
}

// Using this name to avoid clashes with all our other `Label` types.
data class LabelExp(val label: LabelEmbedding) : UnitResultExpEmbedding {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addLabel(label.toViper(ctx))
    }
    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Label", label.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitLabelExp(this)
}

/**
 * An expression that optionally has a label and that uses a goto to exit.
 *
 * The result of the intermediate expression is stored.
 */
data class GotoChainNode(val label: LabelEmbedding?, val exp: ExpEmbedding, val next: LabelLink) : OptionalResultExpEmbedding {
    override val type: TypeEmbedding = exp.type
    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        label?.let { ctx.addLabel(it.toViper(ctx)) }
        ctx.addStatement {
            exp.toViperMaybeStoringIn(result, ctx)
            next.toViperGoto(ctx)
        }
    }
    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("GotoChainNode", listOfNotNull())

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(exp)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitGotoChainNode(this)
}

data class NonDeterministically(val exp: ExpEmbedding) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addStatement {
            val choice = ctx.freshAnonVar { boolean() }
            val expViper = ctx.asBlock { exp.toViper(this) }
            Stmt.If(choice.toViperBuiltinType(ctx), expViper, Stmt.Seqn(), ctx.source.asPosition)
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitNonDeterministically(this)
}

// Note: this is always a *real* Viper method call.
data class MethodCall(val method: NamedFunctionSignature, val args: List<ExpEmbedding>) : StoredResultExpEmbedding {
    override val type: TypeEmbedding = method.callableType.returnType
    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.addStatement {
            method.toMethodCall(
                args.map { it.toViper(ctx) },
                result.toLocalVarUse(ctx.source.asPosition),
                ctx.source.asPosition
            )
        }
    }
    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "MethodCall",
            buildList {
                add(method.nameAsDebugTreeView.withDesignation("callee"))
                addAll(args.map { it.debugTreeView })
            })

    override fun children(): Sequence<ExpEmbedding> = args.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitMethodCall(this)
}

/**
 * We need to generate a fresh variable here since we want to havoc the result.
 *
 * TODO: do this with an explicit havoc in `toViperMaybeStoringIn`.
 */
data class InvokeFunctionObject(val receiver: ExpEmbedding, val args: List<ExpEmbedding>, override val type: TypeEmbedding) :
    OnlyToViperExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(type)
        receiver.toViperUnusedResult(ctx)
        for (arg in args) arg.toViperUnusedResult(ctx)
        // TODO: figure out which exactly invariants we want here
        return variable.withInvariants {
            proven = true
            access = true
        }.toViper(ctx)
    }
    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "InvokeFunctionObject",
            buildList {
                add(receiver.debugTreeView.withDesignation("receiver"))
                addAll(args.map { it.debugTreeView })
            })

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitInvokeFunctionObject(this)
}

data class FunctionExp(val signature: FullNamedFunctionSignature?, val body: ExpEmbedding, val returnLabel: LabelEmbedding) :
    OptionalResultExpEmbedding {
    override val type: TypeEmbedding = body.type
    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        signature?.formalArgs?.forEach { arg ->
            // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
            // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
            // provenInvariants don't contain permissions.
            // TODO (inhale vs require) Decide if `predicateAccessInvariant` should be required rather than inhaled in the beginning of the body.
            (arg.provenInvariants() + listOfNotNull(arg.sharedPredicateAccessInvariant())).forEach { invariant ->
                ctx.addStatement { Stmt.Inhale(invariant.toViperBuiltinType(ctx), ctx.source.asPosition) }
            }
        }
        body.toViperMaybeStoringIn(result, ctx)
        ctx.addLabel(returnLabel.toViper(ctx))
    }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "Function",
            listOfNotNull(
                signature?.nameAsDebugTreeView?.withDesignation("name"),
                body.debugTreeView,
                returnLabel.debugTreeView.withDesignation("return")
            )
        )

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(body)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFunctionExp(this)
}

data class Elvis(val left: ExpEmbedding, val right: ExpEmbedding, override val type: TypeEmbedding) : StoredResultExpEmbedding,
    DefaultDebugTreeViewImplementation {
    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        val leftViper = left.toViper(ctx)
        val leftWrapped = ExpWrapper(leftViper, left.type)
        val conditional = If(leftWrapped.notNullCmp(), leftWrapped, right, type)
        conditional.toViperStoringIn(result, ctx)
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(left, right)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitElvis(this)
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.asInfo
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.embeddings.types.injectionOr
import org.jetbrains.kotlin.formver.core.linearization.InhaleExhaleStmtModifier
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.core.linearization.UnfoldPolicy
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.purity.PurityContext
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.mangled

sealed interface ExpEmbedding : DebugPrintable {
    val type: TypeEmbedding

    /**
     * The original Kotlin source's role for the generated expression embedding.
     */
    val sourceRole: SourceRole?
        get() = null

    /**
     * Convert this `ExpEmbedding` into a Viper `Exp` of type `Ref`, using the provided context for auxiliary statements and declarations.
     * This should never be used for assertions inside Viper's `inhale`, `exhale`, `require`, `assert` etc.
     *
     * The `Exp` returned contains the result of the expression.
     */
    fun toViper(ctx: LinearizationContext): Exp

    /**
     * Like `toViper`, but store the result in `result`.
     *
     * `result` must be assignable, i.e. a variable or a field access.
     *
     * This function is intended for cases when having the variable already provides some benefit, e.g. in an if statement.
     */
    fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext)

    /**
     * Like `toViperStoringIn`, but allow special handling of the case when the result is unused.
     */
    fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext)

    /**
     * Use in contexts where Viper assertions are needed, e.g. `inhale`, `require`, `ensure`.
     *
     * Tries to return built-in Viper Type (`Bool`, `Int`) instead of `Ref` which is used by default for the representation of objects.
     * When impossible still returns `Ref`. Note that this function should never fail unlike `toViper` which can't convert some `ExpEmbedding`s
     * e.g. access permissions.
     */
    fun toViperBuiltinType(ctx: LinearizationContext): Exp

    /**
     * Like `toViper`, but assume the result is unused.
     */
    fun toViperUnusedResult(ctx: LinearizationContext)

    fun ignoringCasts(): ExpEmbedding = this

    /**
     * Meta nodes are nodes like `WithPosition`.
     */
    fun ignoringMetaNodes(): ExpEmbedding = this

    // TODO: Come up with a better way to solve the problem these `ignoring` functions solve...
    // Probably either virtual functions or a visitor.
    fun ignoringCastsAndMetaNodes(): ExpEmbedding = this

    fun children(): Sequence<ExpEmbedding> = emptySequence()
    fun <R> accept(v: ExpVisitor<R>): R
    fun isValid(ctx: PurityContext): Boolean = true
}

sealed class ToViperBuiltinMisuseError(msg: String) : RuntimeException(msg)

class ToViperBuiltinOnlyError(exp: ExpEmbedding) :
    ToViperBuiltinMisuseError("${exp.debugTreeView.print()} can only be translated to Viper built-in type")

/**
 * `ExpEmbedding` with default translation from Ref to Viper built-in type.
 * Currently `Int` and `Bool` are supported. All other types are left intact.
 */
sealed interface DefaultToBuiltinExpEmbedding : ExpEmbedding {
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
        val exp = toViper(ctx)
        val injection = type.injectionOr { return exp }
        // optimisation here is widely used, in such `ExpEmbedding`s like `Is`
        // (which is very common when inhaling)
        return if (exp is Exp.DomainFuncApp && exp.function == injection.toRef)
            exp.args[0]
        else injection.fromRef(exp, pos = ctx.source.asPosition, info = sourceRole.asInfo)
    }
}

/**
 * Default implementation for `toViperStoringIn`, which simply assigns the value to the result variable.
 */
sealed interface DefaultStoringInExpEmbedding : ExpEmbedding {
    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.addStatement { Stmt.assign(result.toViper(ctx), toViper(ctx)) }
    }
}

/**
 * Default `toViperMaybeStoringIn` implementation, which uses `StoringIn` if there is a result and `UnusedResult` otherwise.
 */
sealed interface DefaultMaybeStoringInExpEmbedding : ExpEmbedding {
    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }
}

/**
 * Default `toViperUnusedResult` implementation, that simply uses `toViper`.
 */
sealed interface DefaultUnusedResultExpEmbedding : ExpEmbedding {
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViper(ctx)
    }
}

sealed interface OnlyToViperExpEmbedding : DefaultStoringInExpEmbedding, DefaultMaybeStoringInExpEmbedding,
    DefaultUnusedResultExpEmbedding,
    DefaultToBuiltinExpEmbedding


/**
 * Default `debugTreeView` implementation that collects trees from a number of possible formats.
 *
 * This covers most use-cases.
 * We don't give `debugAnonymousSubexpressions` a default value since not specifying it explicitly is a good sign we just forgot
 * to implement things for that class.
 */
sealed interface DefaultDebugTreeViewImplementation : ExpEmbedding {
    val debugName: String
        get() = javaClass.simpleName
    val debugAnonymousSubexpressions: List<ExpEmbedding>
    val debugNamedSubexpressions: Map<String, ExpEmbedding>
        get() = mapOf()
    val debugExtraSubtrees: List<TreeView>
        get() = listOf()
    override val debugTreeView: TreeView
        get() {
            val anonymousSubtrees = debugAnonymousSubexpressions.map { it.debugTreeView }
            val namedSubtrees =
                debugNamedSubexpressions.map {
                    designatedNode(
                        it.key,
                        it.value.debugTreeView
                    )
                }
            val allSubtrees = anonymousSubtrees + namedSubtrees + debugExtraSubtrees
            return if (allSubtrees.isNotEmpty()) NamedBranchingNode(debugName, allSubtrees)
            else PlaintextLeaf(debugName)
        }
}

/**
 * `ExpEmbedding` that produces a result as an expression.
 *
 * The best possible implementation of `toViperStoringIn` simply assigns to the result; we cannot
 * propagate the variable in any way.
 */
sealed interface DirectResultExpEmbedding : DefaultMaybeStoringInExpEmbedding, DefaultStoringInExpEmbedding,
    DefaultDebugTreeViewImplementation, DefaultToBuiltinExpEmbedding {
    /**
     * When the result is unused, we don't want to produce any expression, but we still want to evaluate the subexpressions.
     */
    val subexpressions: List<ExpEmbedding>
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        for (exp in subexpressions) {
            exp.toViperUnusedResult(ctx)
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = subexpressions

    override fun children(): Sequence<ExpEmbedding> = subexpressions.asSequence()
}

/**
 * `ExpEmbedding`s that can only be a part of a Viper assertion, e.g. field access permissions.
 */
sealed interface OnlyToBuiltinTypeExpEmbedding : DirectResultExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp = throw ToViperBuiltinOnlyError(this)
}

sealed interface NullaryDirectResultExpEmbedding : DirectResultExpEmbedding {
    override val subexpressions: List<ExpEmbedding>
        get() = listOf()
}

sealed interface UnaryDirectResultExpEmbedding : DirectResultExpEmbedding {
    val inner: ExpEmbedding

    override val subexpressions: List<ExpEmbedding>
        get() = listOf(inner)
}

sealed interface BinaryDirectResultExpEmbedding : DirectResultExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding

    override val subexpressions: List<ExpEmbedding>
        get() = listOf(left, right)
}

/**
 * `ExpEmbedding` that requires a location to store its result.
 *
 * The best possible implementation of `toViper` is to generate a fresh location and place the result there.
 */
sealed interface BaseStoredResultExpEmbedding : ExpEmbedding, DefaultToBuiltinExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(type)
        toViperStoringIn(variable, ctx)
        return variable.toViper(ctx)
    }
}

/**
 * `ExpEmbedding` that always produces and stores a result, even if that result is unused.
 */
sealed interface StoredResultExpEmbedding : BaseStoredResultExpEmbedding, DefaultMaybeStoringInExpEmbedding,
    DefaultUnusedResultExpEmbedding

/**
 * `ExpEmbedding` that does not evaluate to a value, i.e. does not produce any result (not even `Unit`).
 *
 * Examples are `return`, `break`, `continue`...
 */
sealed interface NoResultExpEmbedding : DefaultMaybeStoringInExpEmbedding, DefaultToBuiltinExpEmbedding {
    override val type: TypeEmbedding
        get() = buildType { nothing() }

    // Result ignored, since it is never used.
    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        toViperUnusedResult(ctx)
    }

    override fun toViper(ctx: LinearizationContext): Exp {
        toViperUnusedResult(ctx)
        return RuntimeTypeDomain.unitValue(pos = ctx.source.asPosition)
    }
}

/**
 * `ExpEmbedding` that can be converted to an `Exp` without any linearization context.
 *
 * Note that such an expression of course cannot have (non-pure) subexpressions, since otherwise they would have to be linearized as well.
 */
sealed interface PureExpEmbedding : NullaryDirectResultExpEmbedding {
    fun toViper(source: KtSourceElement? = null): Exp
    override fun toViper(ctx: LinearizationContext): Exp = toViper(ctx.source)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPureExpEmbedding(this)
}

/**
 * `ExpEmbedding` with different behaviour when there is and isn't a result.
 *
 * These are typically control flow structures like `if`.
 */
sealed interface OptionalResultExpEmbedding : BaseStoredResultExpEmbedding {
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViperMaybeStoringIn(null, ctx)
    }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        toViperMaybeStoringIn(result, ctx)
    }
}

/**
 * `ExpEmbedding` that wraps another `ExpEmbedding` and delegates all the generation to the inner one.
 *
 * The embedding can still modify the context, which is the main use for this type of embedding.
 */
sealed interface PassthroughExpEmbedding : ExpEmbedding {
    val inner: ExpEmbedding
    override val type: TypeEmbedding
        get() = inner.type

    override fun toViper(ctx: LinearizationContext): Exp = withPassthroughHook(ctx) { inner.toViper(this) }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp = withPassthroughHook(ctx) {
        inner.toViperBuiltinType(this)
    }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        withPassthroughHook(ctx) {
            inner.toViperStoringIn(result, this)
        }
    }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        withPassthroughHook(ctx) {
            inner.toViperMaybeStoringIn(result, this)
        }
    }

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        withPassthroughHook(ctx) {
            inner.toViperUnusedResult(this)
        }
    }

    fun <R> withPassthroughHook(ctx: LinearizationContext, action: LinearizationContext.() -> R): R

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}

/**
 * `ExpEmbedding` that always evaluates to `Unit`.
 */
sealed interface UnitResultExpEmbedding : OnlyToViperExpEmbedding {
    override val type: TypeEmbedding
        get() = buildType { unit() }

    override fun toViper(ctx: LinearizationContext): Exp {
        toViperSideEffects(ctx)
        return RuntimeTypeDomain.unitValue(pos = ctx.source.asPosition)
    }

    fun toViperSideEffects(ctx: LinearizationContext)
}

fun List<ExpEmbedding>.toViper(ctx: LinearizationContext): List<Exp> = map { it.toViper(ctx) }

/**
 * Field access that does not care about permissions.
 *
 * This is convenient to have for implementing the other operations.
 */
data class PrimitiveFieldAccess(override val inner: ExpEmbedding, val field: FieldEmbedding) :
    UnaryDirectResultExpEmbedding,
    DefaultToBuiltinExpEmbedding {
    override val type: TypeEmbedding
        get() = this.field.type

    override fun toViper(ctx: LinearizationContext): Exp =
        Exp.FieldAccess(inner.toViper(ctx), field.toViper(), ctx.source.asPosition)

    override val debugTreeView: TreeView
        get() = OperatorNode(inner.debugTreeView, ".", this.field.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPrimitiveFieldAccess(this)
}

data class FieldAccess(val receiver: ExpEmbedding, val field: FieldEmbedding) : DefaultMaybeStoringInExpEmbedding,
    DefaultToBuiltinExpEmbedding {
    override val type: TypeEmbedding = field.type
    private val accessInvariant = field.accessInvariantForAccess()
    private val noInvariants: Boolean
        get() = accessInvariant == null

    override fun toViper(ctx: LinearizationContext): Exp {
        if (noInvariants && !field.unfoldToAccess) return Exp.FieldAccess(
            receiver.toViper(ctx),
            field.toViper(),
            ctx.source.asPosition
        )

        if (field.unfoldToAccess && ctx.unfoldPolicy == UnfoldPolicy.UNFOLDING_IN) return unfoldingInImpl(ctx)
        val variable = ctx.freshAnonVar(type)
        toViperStoringIn(variable, ctx)
        return variable.toViper(ctx)
    }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        val receiverViper = receiver.toViper(ctx)
        val receiverWrapper = ExpWrapper(receiverViper, receiver.type)
        // If the field is immutable, it is necessary to unfold predicates
        if (field.unfoldToAccess) unfoldHierarchy(receiverWrapper, ctx)

        val fieldAccess = PrimitiveFieldAccess(receiverWrapper, field)
        val invariant = accessInvariant?.fillHole(receiverWrapper)?.pureToViper(toBuiltin = true, ctx.source)
        ctx.addStatement {
            invariant?.let { addModifier(InhaleExhaleStmtModifier(it)) }
            Stmt.assign(
                result.toViper(ctx),
                fieldAccess.pureToViper(toBuiltin = false, ctx.source),
                ctx.source.asPosition
            )
        }
    }

    private fun unfoldingInImpl(ctx: LinearizationContext): Exp {
        val hierarchyPath = (receiver.type.pretype as? ClassTypeEmbedding)?.details?.hierarchyUnfoldPath(field)
        val primitiveAccess: Exp = Exp.FieldAccess(receiver.toViper(ctx), field.toViper(), ctx.source.asPosition)
        if (hierarchyPath == null) return primitiveAccess
        return hierarchyPath.toList().foldRight(primitiveAccess) { classType, acc ->
            val predAcc = classType.predicateAccess(receiver, ctx)
            Exp.Unfolding(predAcc, acc)
        }
    }

    private fun ClassTypeEmbedding.predicateAccess(
        receiver: ExpEmbedding,
        ctx: LinearizationContext
    ): Exp.PredicateAccess =
        sharedPredicateAccessInvariant().fillHole(receiver)
            .pureToViper(toBuiltin = true, ctx.source) as? Exp.PredicateAccess
            ?: error("Attempt to unfold a predicate of ${name.mangled}.")

    private fun unfoldHierarchy(receiverWrapper: ExpEmbedding, ctx: LinearizationContext) {
        val hierarchyPath = (receiver.type.pretype as? ClassTypeEmbedding)?.details?.hierarchyUnfoldPath(field)
        hierarchyPath?.forEach { classType ->
            val predAcc = classType.predicateAccess(receiverWrapper, ctx)
            predAcc.let { ctx.addStatement { Stmt.Unfold(it) } }
        }
    }

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        receiver.toViperUnusedResult(ctx)
    }

    override val debugTreeView: TreeView
        get() = OperatorNode(receiver.debugTreeView, ".", this.field.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldAccess(this)
}

/**
 * Represents a combination of `Assign` + `FieldAccess`.
 */
data class FieldModification(val receiver: ExpEmbedding, val field: FieldEmbedding, val newValue: ExpEmbedding) :
    UnitResultExpEmbedding {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        val receiverViper = receiver.toViper(ctx)
        val newValueViper = newValue.withType(field.type).toViper(ctx)
        val invariant =
            field.accessInvariantForAccess()?.fillHole(ExpWrapper(receiverViper, receiver.type))
                ?.pureToViper(toBuiltin = true, ctx.source)

        ctx.addStatement {
            invariant?.let { addModifier(InhaleExhaleStmtModifier(it)) }
            Stmt.FieldAssign(Exp.FieldAccess(receiverViper, field.toViper()), newValueViper, ctx.source.asPosition)
        }
    }

    override val debugTreeView: TreeView
        get() = OperatorNode(
            OperatorNode(receiver.debugTreeView, ".", this.field.debugTreeView),
            " := ",
            newValue.debugTreeView
        )

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldModification(this)
}

data class FieldAccessPermissions(override val inner: ExpEmbedding, val field: FieldEmbedding, val perm: PermExp) :
    OnlyToBuiltinTypeExpEmbedding, UnaryDirectResultExpEmbedding {
    // We consider access permissions to have type Boolean, though this is a bit questionable.
    override val type: TypeEmbedding = buildType { boolean() }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        inner.toViper(ctx).fieldAccessPredicate(field.toViper(), perm, ctx.source.asPosition)

    // field collides with the field context-sensitive keyword.
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(this.field.debugTreeView, perm.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldAccessPermissions(this)
}

// Ideally we would use the predicate, but due to the possibility of recursion this is inconvenient at present.
data class PredicateAccessPermissions(val predicateName: MangledName, val args: List<ExpEmbedding>, val perm: PermExp) :
    OnlyToBuiltinTypeExpEmbedding {
    override val type: TypeEmbedding = buildType { boolean() }
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        Exp.PredicateAccess(predicateName, args.map { it.toViper(ctx) }, perm, ctx.source.asPosition)

    override val subexpressions: List<ExpEmbedding>
        get() = args

    override val debugTreeView: TreeView
        get() = NamedBranchingNode("PredicateAccess", buildList {
            add(PlaintextLeaf(predicateName.mangled).withDesignation("name"))
            addAll(args.map { it.debugTreeView })
        })

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPredicateAccessPermissions(this)
}

data class Assign(val lhs: ExpEmbedding, val rhs: ExpEmbedding) : UnitResultExpEmbedding {
    override val type: TypeEmbedding = lhs.type

    override fun toViperSideEffects(ctx: LinearizationContext) {
        val lhsViper = lhs.toViper(ctx)
        if (lhsViper is Exp.LocalVar) {
            rhs.withType(lhs.type).toViperStoringIn(LinearizationVariableEmbedding(lhsViper.name, lhs.type), ctx)
        } else {
            val rhsViper = rhs.withType(lhs.type).toViper(ctx)
            ctx.addStatement { Stmt.assign(lhsViper, rhsViper, ctx.source.asPosition) }
        }
    }

    override val debugTreeView: TreeView
        get() = OperatorNode(lhs.debugTreeView, " := ", rhs.debugTreeView)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(lhs, rhs)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAssign(this)
}

data class Declare(val variable: VariableEmbedding, val initializer: ExpEmbedding?) : UnitResultExpEmbedding,
    DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { unit() }

    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addDeclaration(variable.toLocalVarDecl(ctx.source.asPosition))
        initializer?.toViperStoringIn(variable, ctx)
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()

    override val debugExtraSubtrees: List<TreeView>
        get() = listOfNotNull(variable.debugTreeView, variable.type.debugTreeView, initializer?.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitDeclare(this)
}

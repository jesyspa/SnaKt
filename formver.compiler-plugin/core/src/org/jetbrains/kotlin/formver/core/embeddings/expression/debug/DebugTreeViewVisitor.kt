/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression.debug

import org.jetbrains.kotlin.formver.core.embeddings.DefaultingExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.mangled

class DebugTreeViewVisitor(private val nameResolver: NameResolver) : DefaultingExpVisitor<TreeView> {

    private fun ExpEmbedding.tree(): TreeView = accept(this@DebugTreeViewVisitor)
    private fun List<ExpEmbedding>.trees(): List<TreeView> = map { it.tree() }

    // region Helpers for the DefaultDebugTreeViewImplementation pattern

    private fun defaultTree(
        name: String,
        anonymous: List<ExpEmbedding> = emptyList(),
        named: Map<String, ExpEmbedding> = emptyMap(),
        extraSubtrees: List<TreeView> = emptyList(),
    ): TreeView {
        val anonymousSubtrees = anonymous.trees()
        val namedSubtrees = named.map { designatedNode(it.key, it.value.tree()) }
        val allSubtrees = anonymousSubtrees + namedSubtrees + extraSubtrees
        return if (allSubtrees.isNotEmpty()) NamedBranchingNode(name, allSubtrees)
        else PlaintextLeaf(name)
    }

    // endregion


    // region Permissions

    override fun visitFold(e: Fold): TreeView = with(nameResolver) {
        defaultTree(
            "Fold",
            extraSubtrees = listOf(
                e.pred.debugTreeView
            ),
        )
    }

    override fun visitUnfold(e: Unfold): TreeView = with(nameResolver) {
        defaultTree(
            "Unfold",
            extraSubtrees = listOf(
                e.pred.debugTreeView
            ),
        )
    }

    // endregion

    // region Control Flow

    override fun visitBlock(e: Block): TreeView =
        BlockNode(e.exps.trees())

    override fun visitWhile(e: While): TreeView = with(nameResolver) {
        defaultTree(
            "While",
            listOf(e.condition, e.body),
            extraSubtrees = listOf(
                e.breakLabel.debugTreeView.withDesignation("break"),
                e.continueLabel.debugTreeView.withDesignation("continue"),
            ),
        )
    }

    override fun visitGoto(e: Goto): TreeView = with(nameResolver) {
        defaultTree("Goto", extraSubtrees = listOf(e.target.debugTreeView))
    }

    override fun visitLabelExp(e: LabelExp): TreeView = with(nameResolver) {
        NamedBranchingNode("Label", e.label.debugTreeView)
    }

    override fun visitGotoChainNode(e: GotoChainNode): TreeView =
        NamedBranchingNode("GotoChainNode", listOfNotNull())

    override fun visitMethodCall(e: MethodCall): TreeView = with(nameResolver) {
        NamedBranchingNode(
            "MethodCall",
            buildList {
                add(e.method.nameAsDebugTreeView.withDesignation("callee"))
                addAll(e.args.trees())
            },
        )
    }

    override fun visitInvokeFunctionObject(e: InvokeFunctionObject): TreeView =
        NamedBranchingNode(
            "InvokeFunctionObject",
            buildList {
                add(e.receiver.tree().withDesignation("receiver"))
                addAll(e.args.trees())
            },
        )

    override fun visitFunctionExp(e: FunctionExp): TreeView = with(nameResolver) {
        NamedBranchingNode(
            "Function",
            buildList {
                e.signature?.let { add(it.nameAsDebugTreeView.withDesignation("name")) }
                add(e.body.tree())
                add(e.returnLabel.debugTreeView.withDesignation("return"))
            },
        )
    }

    override fun visitReturn(e: Return): TreeView =
        NamedBranchingNode("Return", listOf(e.target.variable.tree(), e.returnExp.tree()))

    // endregion

    // region Variables

    override fun visitVariableEmbedding(e: VariableEmbedding): TreeView = with(nameResolver) {
        NamedBranchingNode("Var", PlaintextLeaf(e.name.mangled))
    }

    // endregion

    // region Literals

    override fun visitLiteralEmbedding(e: LiteralEmbedding): TreeView =
        defaultTree(e.debugName, extraSubtrees = listOf(PlaintextLeaf(e.value.toString())))

    override fun visitUnitLit(e: UnitLit): TreeView =
        visitLiteralEmbedding(e)

    // endregion

    // region Field Access

    override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): TreeView = with(nameResolver) {
        OperatorNode(e.inner.tree(), ".", e.field.debugTreeView)
    }

    override fun visitFieldAccess(e: FieldAccess): TreeView = with(nameResolver) {
        OperatorNode(e.receiver.tree(), ".", e.field.debugTreeView)
    }

    override fun visitAllocateObject(e: AllocateObject): TreeView = with(nameResolver) {
        OperatorNode(
            e.variable.tree(),
            " := new ",
            e.classType.debugTreeView,
        )
    }

    override fun visitInitField(e: InitField): TreeView = with(nameResolver) {
        OperatorNode(
            OperatorNode(e.receiver.tree(), ".", e.field.debugTreeView),
            " := ",
            e.value.tree(),
        )
    }

    override fun visitFieldModification(e: FieldModification): TreeView = with(nameResolver) {
        OperatorNode(
            OperatorNode(e.receiver.tree(), ".", e.field.debugTreeView),
            " := ",
            e.newValue.tree(),
        )
    }

    override fun visitFieldAccessPermissions(e: FieldAccessPermissions): TreeView = with(nameResolver) {
        defaultTree(
            "FieldAccessPermissions",
            extraSubtrees = listOf(e.field.debugTreeView, e.perm.debugTreeView),
        )
    }

    override fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): TreeView = with(nameResolver) {
        NamedBranchingNode(
            "PredicateAccess",
            buildList {
                add(PlaintextLeaf(e.predicateName.mangled))
                addAll(e.args.trees())
            },
        )
    }

    // endregion

    // region Assignments

    override fun visitAssign(e: Assign): TreeView =
        OperatorNode(e.lhs.tree(), " := ", e.rhs.tree())

    override fun visitDeclare(e: Declare): TreeView = with(nameResolver) {
        defaultTree(
            "Declare",
            extraSubtrees = listOfNotNull(
                e.variable.tree(),
                e.variable.type.debugTreeView,
                e.initializer?.tree(),
            ),
        )
    }

    // endregion

    // region Type Operations

    override fun visitIs(e: Is): TreeView = with(nameResolver) {
        defaultTree(
            "Is",
            listOf(e.inner),
            extraSubtrees = listOf(e.comparisonType.debugTreeView.withDesignation("type")),
        )
    }

    override fun visitCast(e: Cast): TreeView = with(nameResolver) {
        defaultTree(
            "Cast",
            listOf(e.inner),
            extraSubtrees = listOf(e.type.debugTreeView.withDesignation("target")),
        )
    }

    override fun visitSafeCast(e: SafeCast): TreeView = with(nameResolver) {
        defaultTree(
            "SafeCast",
            listOf(e.exp),
            extraSubtrees = listOf(e.targetType.debugTreeView.withDesignation("type")),
        )
    }

    override fun visitInhaleInvariants(e: InhaleInvariants): TreeView = with(nameResolver) {
        defaultTree(
            "InhaleInvariants",
            listOf(e.exp),
            extraSubtrees = listOf(e.type.debugTreeView.withDesignation("type")),
        )
    }

    // endregion

    // region Operators

    override fun visitInjectionBasedExpEmbedding(e: InjectionBasedExpEmbedding): TreeView =
        error("visitInjectionBasedExpEmbedding should not be called directly")

    // endregion

    // region Meta

    override fun visitWithPosition(e: WithPosition): TreeView =
        e.inner.tree()

    override fun visitSharingContext(e: SharingContext): TreeView =
        NamedBranchingNode(
            "SharingContext",
            e.inner.tree(),
            PlaintextLeaf(System.identityHashCode(e).toString()).withDesignation("ctxId"),
        )

    override fun visitShared(e: Shared): TreeView =
        NamedBranchingNode(
            "Shared",
            e.inner.tree(),
            PlaintextLeaf(System.identityHashCode(e.context).toString()).withDesignation("ctxId"),
        )

    // endregion

    // region Lambda

    override fun visitLambdaExp(e: LambdaExp): TreeView =
        PlaintextLeaf("Lambda")

    // endregion

    override fun visitDefault(e: ExpEmbedding): TreeView =
        defaultTree(e.javaClass.simpleName, e.children().toList())
}

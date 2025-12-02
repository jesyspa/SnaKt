/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class PureLinearizerMisuseException(val offendingFunction: String) : IllegalStateException(offendingFunction)

/**
 * Linearization context that does not permit generation of statements.
 *
 * There are cases in Viper where we expect our result to be an expression by itself, for example when
 * processing preconditions, postconditions, and invariants. In those cases, generating statements
 * would be an error.
 */
class PureLinearizer(override val source: KtSourceElement?, val letChainBuilder: LetChainBuilder) :
    LinearizationContext {
    override val unfoldPolicy: UnfoldPolicy
        get() = UnfoldPolicy.UNFOLDING_IN

    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_EXPRESSION

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        PureLinearizer(newSource, letChainBuilder).action()

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding {
        throw PureLinearizerMisuseException("newVar")
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        throw PureLinearizerMisuseException("withNewScopeToBlock")
    }

    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        throw PureLinearizerMisuseException("addStatement")
    }

    // Nothing to do here, as an assignment is also added
    override fun addDeclaration(decl: Declaration) {}

    override fun addAssignment(lhs: ExpEmbedding, rhs: ExpEmbedding) {
        // TODO: Move this to purity check
        if (lhs.ignoringMetaNodes() !is VariableEmbedding) throw SnaktInternalException(
            source,
            "A pure expression expects the lhs of an assignment to be a variable. Offending embedidng: $lhs"
        )
        letChainBuilder.addAssignment(
            (lhs.ignoringMetaNodes() as VariableEmbedding).toLocalVarDecl(),
            rhs.toViper(this)
        )
    }

    override fun addModifier(mod: StmtModifier) {
        throw PureLinearizerMisuseException("addModifier")
    }
}

fun ExpEmbedding.pureToViper(toBuiltin: Boolean, source: KtSourceElement? = null): Exp {
    try {
        val letChainBuilder = LetChainBuilder(source)
        val linearizer = PureLinearizer(source, letChainBuilder)
        return if (toBuiltin) toViperBuiltinType(linearizer) else toViper(linearizer)
    } catch (e: PureLinearizerMisuseException) {
        val catchNameResolver = SimpleNameResolver()
        val debugView = with(catchNameResolver) { debugTreeView.print() }
        val msg =
            "PureLinearizer used to convert non-pure ExpEmbedding; operation ${e.offendingFunction} is not supported in a pure context.\nEmbedding debug view:\n${debugView}"
        throw IllegalStateException(msg)
    }
}

fun List<ExpEmbedding>.pureToViper(toBuiltin: Boolean, source: KtSourceElement? = null): List<Exp> =
    map { it.pureToViper(toBuiltin, source) }

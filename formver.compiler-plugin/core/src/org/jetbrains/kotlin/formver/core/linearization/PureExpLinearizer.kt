/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class PureExpLinearizerMisuseException(val offendingFunction: String) : IllegalStateException(offendingFunction)

/**
 * Linearization context linearizing a pure ExpEmbedding into a Viper expression.
 *
 * There are cases in Viper where we expect our result to be an expression by itself, for example when
 * processing preconditions, postconditions, and invariants. Compared to the [PureFunBodyLinearizer],
 * this linearizer is highly restrictive on what embeddings are supported and is used to translate
 * specifications made in Kotlin into Viper expressions.
 */
data class PureExpLinearizer(
    override val source: KtSourceElement?,
    override val typeResolver: TypeResolver
) : LinearizationContext {

    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_EXPRESSION

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding {
        throw PureExpLinearizerMisuseException("freshAnonVar")
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        throw PureExpLinearizerMisuseException("withNewScopeToBlock")
    }

    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        throw PureExpLinearizerMisuseException("addStatement")
    }

    override fun addDeclaration(decl: Declaration) {
        throw PureExpLinearizerMisuseException("addDeclaration")
    }

    override fun store(lhs: VariableEmbedding, rhs: Linearizable) {
        throw PureExpLinearizerMisuseException("store")
    }

    override fun addReturn(returnExp: Linearizable, target: ReturnTarget) {
        throw PureExpLinearizerMisuseException("addReturn")
    }

    override fun addBranch(
        condition: Linearizable,
        thenBranch: Linearizable,
        elseBranch: Linearizable,
        result: VariableEmbedding?
    ) {
        throw PureExpLinearizerMisuseException("addBranch")
    }

    override fun addFieldAccessStoringIn(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding, result: VariableEmbedding) {
        throw PureExpLinearizerMisuseException("addFieldAccessWithResult")
    }

    override fun addFieldAccess(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding): Exp {
        val receiverViper = receiver.toViper(this)
        val primitiveAccess: Exp = Exp.FieldAccess(receiverViper, field.toViper(), source.asPosition)
        return hierarchyPredicateAccesses(receiverViper, receiverType, field).toList()
            .foldRight(primitiveAccess) { predicateAccess, acc -> Exp.Unfolding(predicateAccess, acc) }
    }

    override fun addModifier(mod: StmtModifier) {
        throw PureExpLinearizerMisuseException("addModifier")
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        name
}

fun ExpEmbedding.pureToViper(toBuiltin: Boolean, typeResolver: TypeResolver, source: KtSourceElement? = null): Exp {
    try {
        val linearizer = PureExpLinearizer(source, typeResolver)
        val lin = toLinearizable(source)
        return if (toBuiltin) lin.toViperBuiltinType(linearizer) else lin.toViper(linearizer)
    } catch (e: PureExpLinearizerMisuseException) {
        val catchNameResolver = SimpleNameResolver()
        val debugView = with(catchNameResolver) { debugTreeView.print() }
        val msg =
            "PureLinearizer used to convert non-pure ExpEmbedding; operation ${e.offendingFunction} is not supported in a pure context.\nEmbedding debug view:\n${debugView}"
        throw IllegalStateException(msg)
    }
}

fun List<ExpEmbedding>.pureToViper(
    toBuiltin: Boolean,
    typeResolver: TypeResolver,
    source: KtSourceElement? = null
): List<Exp> =
    map { it.pureToViper(toBuiltin, typeResolver, source) }

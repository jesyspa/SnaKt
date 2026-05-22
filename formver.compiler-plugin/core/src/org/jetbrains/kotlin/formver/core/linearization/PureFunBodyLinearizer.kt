/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class PureFunBodyLinearizerMisuseException(offendingFunction: String) : IllegalStateException(offendingFunction)

/**
 * Linearization context linearizing a pure ExpEmbedding into a chain of let-bindings
 *
 * Especially when linearizing the embedding of a pure function body this linearization process is used.
 * Compared to the [PureExpLinearizer], this linearizer is intended for use on imperative code to be translated
 * into a single expression in Viper, but not for use in specifications.
 */
data class PureFunBodyLinearizer(
    override val source: KtSourceElement?,
    val state: SharedLinearizationState = SharedLinearizationState(FreshEntityProducer(::AnonymousVariableEmbedding)),
    private val ssaConverter: SsaConverter = SsaConverter(source),
    override val typeResolver: TypeResolver,
) : LinearizationContext {

    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_EXPRESSION

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding = state.freshAnonVar(type)

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        throw PureFunBodyLinearizerMisuseException("withNewScopeToBlock")
    }

    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        throw PureFunBodyLinearizerMisuseException("addStatement")
    }

    override fun addDeclaration(decl: Declaration) {}

    override fun store(lhs: VariableEmbedding, rhs: Linearizable) =
        ssaConverter.addAssignment(lhs.name, rhs.toViper(this))

    override fun addReturn(returnExp: Linearizable, target: ReturnTarget) {
        ssaConverter.addReturn(returnExp.toViper(this))
    }

    override fun addBranch(
        condition: Linearizable,
        thenBranch: Linearizable,
        elseBranch: Linearizable,
        result: VariableEmbedding?
    ) {
        val conditionExp = condition.toViperBuiltinType(this)
        var resultThen: Exp? = null
        var resultElse: Exp? = null

        ssaConverter.branch(
            conditionExp,
            {
                if (result != null) {
                    resultThen = thenBranch.toViper(this)
                } else {
                    thenBranch.toViperUnusedResult(this)
                }
            },
            {
                if (result != null) {
                    resultElse = elseBranch.toViper(this)
                } else {
                    elseBranch.toViperUnusedResult(this)
                }
            })

        if (result != null) {
            if (resultThen == null || resultElse == null) throw SnaktInternalException(
                source,
                "Tried to translate an if-embedding missing a branch"
            )
            ssaConverter.addAssignment(result.name, Exp.TernaryExp(conditionExp, resultThen, resultElse))
        }
    }

    override fun addFieldAccessStoringIn(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding, result: VariableEmbedding) {
        val viperReceiver = receiver.toViper(this)
        if (viperReceiver !is Exp.LocalVar) throw SnaktInternalException(
            source,
            "Invalid receiver encountered in pure function"
        )
        val lowering = FieldAccessLowering(typeResolver, source)
        val accessInvariants =
            lowering.pathTo(receiverType, field).map { lowering.predicateAccessFor(viperReceiver, it) }.toList()
        val primitiveAccess: Exp = Exp.FieldAccess(viperReceiver, field.toViper(), source.asPosition)
        ssaConverter.addAssignment(result.name, primitiveAccess, accessInvariants)
    }

    override fun addFieldAccess(receiver: Linearizable, receiverType: TypeEmbedding, field: FieldEmbedding): Exp {
        val result = freshAnonVar(field.type)
        addFieldAccessStoringIn(receiver, receiverType, field, result)
        return result.toViperExp(this)
    }

    override fun addModifier(mod: StmtModifier) {
        throw PureFunBodyLinearizerMisuseException("addModifier")
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        ssaConverter.resolveVariableName(name)

    fun constructExpression(): Exp = ssaConverter.constructExpression()
}

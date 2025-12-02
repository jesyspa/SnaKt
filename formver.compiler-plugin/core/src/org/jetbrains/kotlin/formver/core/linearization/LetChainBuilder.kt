package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp

class LetChainBuilder(val source: KtSourceElement? = null) : LetChainBuildContext {
    private val assignments: MutableMap<Declaration.LocalVarDecl, Exp> = mutableMapOf()
    override fun asLetChain(body: Exp): Exp =
        assignments.entries.toList().foldRight(body) { (decl, expr), acc ->
            Exp.LetBinding(decl, expr, acc)
        }

    override fun addAssignment(decl: Declaration.LocalVarDecl, varExp: Exp) {
        // TODO: Allow this case using SSA transformation
        if (assignments.containsKey(decl)) throw SnaktInternalException(
            source,
            "Found duplicate variable declaration while constructing a let-chain. Declaration is $decl"
        )
        assignments[decl] = varExp
    }
}
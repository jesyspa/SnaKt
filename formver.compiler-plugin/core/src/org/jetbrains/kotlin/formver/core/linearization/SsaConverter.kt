package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp

class SsaConverter(val source: KtSourceElement? = null) {
    private val assignments: MutableMap<Declaration.LocalVarDecl, Exp> = mutableMapOf()
    private var body: Exp? = null

    fun asExp(): Exp {
        if (body == null) throw SnaktInternalException(
            source,
            "Empty body cannot be converted into let chain"
        )
        return assignments.entries.toList().foldRight(body as Exp) { (decl, expr), acc ->
            Exp.LetBinding(decl, expr, acc)
        }
    }

    fun addAssignment(decl: Declaration.LocalVarDecl, varExp: Exp) {
        // TODO: Allow this case using SSA transformation
        if (assignments.containsKey(decl)) throw SnaktInternalException(
            source,
            "Found duplicate variable declaration while constructing a let-chain. Declaration is $decl"
        )
        assignments[decl] = varExp
    }

    fun addBody(body: Exp) {
        this.body = body
    }
}
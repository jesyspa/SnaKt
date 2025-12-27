package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp

class SsaConverter(
    val source: KtSourceElement? = null,
    val variableIndex: MutableMap<SymbolicName, Int> = mutableMapOf()
) {
    private val assignments: MutableList<Triple<Declaration.LocalVarDecl, Int, Exp>> = mutableListOf()
    private var body: Exp? = null

    fun asExp(): Exp {
        if (body == null) throw SnaktInternalException(
            source,
            "Empty body cannot be converted into let chain"
        )
        return assignments.foldRight(body as Exp) { (decl, ssaIdx, expr), acc ->
            Exp.LetBinding(decl.copy(name = SsaVariableName(decl.name, ssaIdx)), expr, acc)
        }
    }

    fun addAssignment(decl: Declaration.LocalVarDecl, varExp: Exp) {
        variableIndex[decl.name] = variableIndex[decl.name]?.plus(1) ?: 0
        assignments.add(Triple(decl, variableIndex[decl.name] as Int, varExp))
    }

    fun addBody(body: Exp) {
        this.body = body
    }

    fun resolveVariableName(name: SymbolicName): SymbolicName =
    // Fall back to parameter if no assignment in map is found
        // TODO: Fall back to global value numbering before falling back to parameter
        assignments.lastOrNull { it.first.name == name }?.let { SsaVariableName(name, it.second) } ?: name
}
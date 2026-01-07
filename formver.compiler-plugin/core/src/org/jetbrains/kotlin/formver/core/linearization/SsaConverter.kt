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
    private val assignments: MutableList<SsaAssignment> = mutableListOf()
    private var body: Exp? = null

    fun asExp(): Exp {
        val bodyExp = body ?: throw SnaktInternalException(
            source,
            "Empty body cannot be converted into let chain"
        )
        return assignments.foldRight(bodyExp) { (decl, ssaIdx, expr), acc ->
            Exp.LetBinding(decl.copy(name = SsaVariableName(decl.name, ssaIdx)), expr, acc)
        }
    }

    fun addAssignment(decl: Declaration.LocalVarDecl, varExp: Exp) {
        val ssaIdx = (variableIndex[decl.name]?.plus(1) ?: 0)
        variableIndex[decl.name] = ssaIdx
        assignments.add(SsaAssignment(decl, ssaIdx, varExp))
    }

    fun addBody(body: Exp) {
        this.body = body
    }

    /**
     * Resolves the symbolic name to its most recent SSA definition.
     * If no local assignment is found, we assume the provided name is valid
     */
    fun resolveVariableName(name: SymbolicName): SymbolicName =
        // TODO: Fall back to global value numbering before falling back to provided name
        assignments.lastOrNull { it.declaration.name == name }?.let { SsaVariableName(name, it.ssaIdx) } ?: name
}

data class SsaAssignment(val declaration: Declaration.LocalVarDecl, val ssaIdx: Int, val exp: Exp)
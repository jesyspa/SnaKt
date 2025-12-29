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
        if (body == null) throw SnaktInternalException(
            source,
            "Empty body cannot be converted into let chain"
        )
        val bodyExp = body!!
        return assignments.foldRight(bodyExp) { (decl, ssaIdx, expr), acc ->
            Exp.LetBinding(decl.copy(name = SsaVariableName(decl.name, ssaIdx)), expr, acc)
        }
    }

    fun addAssignment(decl: Declaration.LocalVarDecl, varExp: Exp) {
        variableIndex[decl.name] = variableIndex[decl.name]?.plus(1) ?: 0
        assignments.add(SsaAssignment(decl, variableIndex[decl.name] as Int, varExp))
    }

    fun addBody(body: Exp) {
        this.body = body
    }

    /**
     * Resolves a base variable name to the latest SSA variable name used in this or previous converters.
     * The name is resolved as follows:
     * 1. Check and use the name in the scope of this converter
     * 2. If 1. fails, check and use the name in the scope of previous converters
     * 3. If 2. fails use the base name (that is implicitly assume a function parameter is being resolved here)
     * @return the resolved variable name of the provided name
     */
    fun resolveVariableName(name: SymbolicName): SymbolicName =
        // TODO: Fall back to global value numbering before falling back to parameter
        assignments.lastOrNull { it.declaration.name == name }?.let { SsaVariableName(name, it.ssaIdx) } ?: name
}

data class SsaAssignment(val declaration: Declaration.LocalVarDecl, val ssaIdx: Int, val exp: Exp)
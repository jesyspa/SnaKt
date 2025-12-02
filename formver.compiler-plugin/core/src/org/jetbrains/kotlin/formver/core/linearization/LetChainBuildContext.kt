package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface LetChainBuildContext {
    // We have return type Exp here as if no assignments are present the body expression is returned
    fun asLetChain(body: Exp): Exp
    fun addAssignment(decl: Declaration.LocalVarDecl, varExp: Exp)
}
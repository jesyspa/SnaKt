/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.SymbolicName

/** Utility class to simplify writing domain functions and axioms.
 *
 * This is like a `VariableEmbedding` but already at the Viper level, making expressions
 * that involve variables less cumbersome to write.
 */
data class Var(val name: SymbolicName, val type: Type) {
    fun use(): Exp.LocalVar = Exp.LocalVar(name, type)
    fun decl(): Declaration.LocalVarDecl = Declaration.LocalVarDecl(name, type)
}

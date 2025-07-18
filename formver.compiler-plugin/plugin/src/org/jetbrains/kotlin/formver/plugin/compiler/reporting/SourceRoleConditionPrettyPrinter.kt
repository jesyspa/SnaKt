/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.reporting

import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole

object SourceRoleConditionPrettyPrinter {
    private fun FirBasedSymbol<*>.showFirSymbol(): String =
        FirDiagnosticRenderers.DECLARATION_NAME.render(this)

    private fun SourceRole.Condition.Constant.showConstant(): String =
        literal.toString()

    private fun SourceRole.Condition.Negation.showNegation(): String =
        "!${arg.prettyPrint()}"

    private fun SourceRole.Condition.Conjunction.showConjunction(): String {
        val lhsExpr = lhs.prettyPrint()
        val rhsExpr = rhs.prettyPrint()
        return "($lhsExpr && $rhsExpr)"
    }

    private val FirBasedSymbol<*>.shownName: String
        get() = when (this) {
            is FirValueParameterSymbol -> showFirSymbol()
            is FirFunctionSymbol<*> -> "this"
            else -> error("Variable must be either value parameter or function itself in case of receiver.")
        }

    private fun SourceRole.Condition.Disjunction.showDisjunction(): String {
        val lhsExpr = lhs.prettyPrint()
        val rhsExpr = rhs.prettyPrint()
        return "($lhsExpr || $rhsExpr)"
    }

    private fun SourceRole.Condition.IsNull.showIsNull(): String = buildString {
        append(targetVariable.shownName)
        when (negated) {
            true -> append(" != ")
            false -> append(" == ")
        }
        append("null")
    }

    private fun SourceRole.Condition.IsType.showIsType(): String = buildString {
        append(targetVariable.shownName)
        when (negated) {
            true -> append(" !is ")
            false -> append(" is ")
        }
        append(expectedType.renderReadable())
    }

    fun SourceRole.Condition.prettyPrint(): String = when (this) {
        is SourceRole.FirSymbolHolder -> firSymbol.showFirSymbol()
        is SourceRole.Condition.Constant -> showConstant()
        is SourceRole.Condition.Negation -> showNegation()
        is SourceRole.Condition.Conjunction -> showConjunction()
        is SourceRole.Condition.Disjunction -> showDisjunction()
        is SourceRole.Condition.IsNull -> showIsNull()
        is SourceRole.Condition.IsType -> showIsType()
    }
}
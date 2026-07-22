/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.core.isSpecificationCall


/**
 * Returns `true` if [this] context is in an argument position for a specification function.
 */
private fun CheckerContext.isInSpecificationContext(): Boolean =
    callsOrAssignments.any { statement ->
        statement.isSpecificationCall()
    }

/**
 * Wraps a declaration [checker] into a checker that only executes if not in a specification context.
 *
 * @See [CheckerContext.isInSpecificationContext]
 */
class SpecificationAwareDeclarationChecker<Declaration: FirDeclaration>(
    private val checker: FirDeclarationChecker<Declaration>
) : FirDeclarationChecker<Declaration>(checker.mppKind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: Declaration) {
        if (context.isInSpecificationContext()) return

        checker.check(declaration)
    }
}

/**
 * Wraps [this] checker into a [SpecificationAwareDeclarationChecker].
 */
fun <Declaration: FirDeclaration> FirDeclarationChecker<Declaration>.asSpecificationAware()
        : FirDeclarationChecker<Declaration> =
    SpecificationAwareDeclarationChecker(this)

/**
 * Wraps an expression [checker] into a checker that only executes if not in a specification context.
 *
 * @See [CheckerContext.isInSpecificationContext]
 */
class SpecificationAwareExpressionChecker<Expression: FirStatement>(
    private val checker: FirExpressionChecker<Expression>
) : FirExpressionChecker<Expression>(checker.mppKind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: Expression) {
        if (context.isInSpecificationContext()) return

        checker.check(expression)
    }
}

/**
 * Wraps [this] checker into a [SpecificationAwareExpressionChecker].
 */
fun <Expression: FirStatement> FirExpressionChecker<Expression>.asSpecificationAware()
        : FirExpressionChecker<Expression> =
    SpecificationAwareExpressionChecker(this)

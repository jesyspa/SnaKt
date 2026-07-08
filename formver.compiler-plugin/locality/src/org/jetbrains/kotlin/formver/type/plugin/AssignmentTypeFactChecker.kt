/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

/**
 * Checker for type-fact compatibility in variable assignments.
 *
 * @param TypeFact the type-fact class of the variables.
 * @param typeFactJudgment the type-fact judgment to use for checking type-fact compatibility.
 * @param expressionTypeFactResolver the resolver for resolving the type facts of both the left and right-hand
 *  expressions of the assignment.
 * @param diagnosticFactory the diagnostic factory to use for reporting type-fact incompatibility.
 */
class AssignmentTypeFactChecker<TypeFact>(
    kind: MppCheckerKind,
    private val typeFactJudgment: TypeFactJudgment<TypeFact>,
    private val expressionTypeFactResolver: ExpressionTypeFactResolver<TypeFact>,
    private val leftTypeFactResolver: ExpressionTypeFactResolver<TypeFact> = expressionTypeFactResolver,
    private val diagnosticFactory: KtDiagnosticFactory3<String, TypeFact, TypeFact>
) : FirVariableAssignmentChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        val requiredTypeFact = leftTypeFactResolver.resolveTypeFactOf(expression.lValue)
        val actualTypeFact = expressionTypeFactResolver.resolveTypeFactOf(expression.rValue)

        if (typeFactJudgment.satisfies(requiredTypeFact, actualTypeFact)) return

        reporter.reportOn(
            expression.rValue.source,
            diagnosticFactory,
            "Assignment",
            requiredTypeFact,
            actualTypeFact
        )
    }
}

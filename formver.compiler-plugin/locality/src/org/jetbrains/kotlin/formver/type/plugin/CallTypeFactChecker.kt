/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType

/**
 * Checker for type-fact compatibility in function calls.
 *
 * @param TypeFact the type-fact class of the arguments.
 * @param typeFactJudgment the type-fact judgment to use for checking type-fact compatibility.
 * @param expressionTypeFactResolver the resolver for resolving the type facts of the arguments.
 * @param callArgumentTypeFactsMapper for resolving the type-facts of the call's parameters.
 * @param contextDiagnosticFactory the diagnostic factory to use for reporting type-fact incompatibility in context
 *  parameters.
 */
class CallTypeFactChecker<TypeFact>(
    kind: MppCheckerKind,
    private val typeFactJudgment: TypeFactJudgment<TypeFact>,
    private val expressionTypeFactResolver: ExpressionTypeFactResolver<TypeFact>,
    private val callArgumentTypeFactsMapper: CallArgumentTypeFactsMapper<TypeFact>,
    private val argumentDiagnosticFactory: KtDiagnosticFactory3<String, TypeFact, TypeFact>,
    private val contextDiagnosticFactory: KtDiagnosticFactory3<ConeKotlinType, TypeFact, TypeFact>
) : FirCallChecker(kind) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val requiredTypes = callArgumentTypeFactsMapper.mapArgumentTypeFactsOf(expression)

        for ((argument, requiredTypeFact) in requiredTypes) {
            val effectiveArguments = argument.unwrapAndFlattenArgument(flattenArrays = false)

            for (effectiveArgument in effectiveArguments) {
                val actualTypeFact = expressionTypeFactResolver.resolveTypeFactOf(effectiveArgument)

                if (typeFactJudgment.satisfies(requiredTypeFact, actualTypeFact)) continue

                if (argument.source?.kind is KtFakeSourceElementKind.ImplicitContextParameterArgument) {
                    reporter.reportOn(
                        expression.source,
                        contextDiagnosticFactory,
                        argument.resolvedType,
                        requiredTypeFact,
                        actualTypeFact
                    )
                } else {
                    reporter.reportOn(
                        effectiveArgument.source ?: argument.source,
                        argumentDiagnosticFactory,
                        "Argument",
                        requiredTypeFact,
                        actualTypeFact
                    )
                }
            }
        }
    }
}

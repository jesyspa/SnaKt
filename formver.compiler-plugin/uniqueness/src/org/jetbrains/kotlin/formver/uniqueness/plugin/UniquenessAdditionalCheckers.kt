/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirThrowExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class UniquenessAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> UniquenessAdditionalCheckers(session) }
        }
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> =
            setOf(PropertyUniquenessChecker)

        override val functionCheckers: Set<FirFunctionChecker> =
            setOf(
                FunctionEscapeUniquenessConsistencyChecker,
                FunctionExitUniquenessConsistencyChecker,
                FunctionMovedAccessChecker
            )
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> =
            setOf(AssignmentUniquenessChecker)

        override val callCheckers: Set<FirCallChecker> =
            setOf(CallUniquenessChecker)

        override val functionCallCheckers: Set<FirFunctionCallChecker> =
            setOf(FunctionCallArgumentUniquenessCollisionChecker)

        override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> =
            setOf(
                QualifiedAccessUniquenessChecker,
                QualifiedAccessArgumentUniquenessCollisionChecker
            )

        override val returnExpressionCheckers: Set<FirReturnExpressionChecker> =
            setOf(ReturnUniquenessChecker)

        override val throwExpressionCheckers: Set<FirThrowExpressionChecker> =
            setOf(ThrowUniquenessChecker)
    }

    override val typeCheckers: TypeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> =
            setOf(TypeRefUniquenessAttributeChecker)
    }
}

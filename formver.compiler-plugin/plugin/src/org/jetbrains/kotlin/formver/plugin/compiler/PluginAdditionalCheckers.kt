/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirThrowExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.locality.contract.plugin.AssignmentLocalityContractChecker
import org.jetbrains.kotlin.formver.locality.contract.plugin.CallLocalityContractChecker
import org.jetbrains.kotlin.formver.locality.contract.plugin.PropertyLocalityContractChecker
import org.jetbrains.kotlin.formver.locality.contract.plugin.QualifiedAccessLocalityContractChecker
import org.jetbrains.kotlin.formver.locality.contract.plugin.ReturnLocalityContractChecker
import org.jetbrains.kotlin.formver.locality.contract.plugin.ValueParameterLocalityContractChecker
import org.jetbrains.kotlin.formver.locality.plugin.AssignmentLocalityChecker
import org.jetbrains.kotlin.formver.locality.plugin.CallLocalityChecker
import org.jetbrains.kotlin.formver.locality.plugin.PropertyAccessLocalityChecker
import org.jetbrains.kotlin.formver.locality.plugin.PropertyLocalityChecker
import org.jetbrains.kotlin.formver.locality.plugin.QualifiedAccessLocalityChecker
import org.jetbrains.kotlin.formver.locality.plugin.ReturnLocalityChecker
import org.jetbrains.kotlin.formver.locality.plugin.ThrowLocalityChecker
import org.jetbrains.kotlin.formver.locality.plugin.TypeRefLocalityAttributeChecker
import org.jetbrains.kotlin.formver.locality.plugin.ValueParameterLocalityChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.AssignmentUniquenessChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.CallUniquenessChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.FunctionCallArgumentUniquenessCollisionChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.FunctionEscapeUniquenessConsistencyChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.FunctionExitUniquenessConsistencyChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.FunctionUseAfterMoveChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.PropertyUniquenessChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.QualifiedAccessArgumentUniquenessCollisionChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.QualifiedAccessUniquenessChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.ReturnUniquenessChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.ThrowUniquenessChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.TypeRefUniquenessAttributeChecker
import org.jetbrains.kotlin.formver.uniqueness.plugin.ValueParameterUniquenessChecker

class PluginAdditionalCheckers(session: FirSession, config: PluginConfiguration) :
    FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(config: PluginConfiguration): Factory {
            return Factory { session -> PluginAdditionalCheckers(session, config) }
        }
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
            get() = buildSet {
                add(ViperPoweredDeclarationChecker(session, config))

                if (config.dumpUniquenessCFG) {
                    add(FunctionUniquenessStateRenderingChecker)
                }
            }

        override val functionCheckers: Set<FirFunctionChecker> = buildSet {
            if (!config.checkUniqueness) return@buildSet

            // Because specification bodies are pure we don't need to check flow-sensitive properties.
            add(FunctionEscapeUniquenessConsistencyChecker.asSpecificationAware())
            add(FunctionExitUniquenessConsistencyChecker.asSpecificationAware())
            add(FunctionUseAfterMoveChecker.asSpecificationAware())
        }

        override val propertyCheckers: Set<FirPropertyChecker> = buildSet {
            if (config.checkLocality) {
                add(PropertyLocalityChecker)
                add(PropertyLocalityContractChecker)
            }

            if (config.checkUniqueness) {
                add(PropertyUniquenessChecker)
            }
        }

        override val valueParameterCheckers: Set<FirValueParameterChecker> = buildSet {
            if (config.checkLocality) {
                add(ValueParameterLocalityChecker)
                add(ValueParameterLocalityContractChecker)
            }

            if (config.checkUniqueness) {
                add(ValueParameterUniquenessChecker)
            }
        }
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = buildSet {
            if (config.checkLocality) {
                add(AssignmentLocalityChecker)
                add(AssignmentLocalityContractChecker)
            }

            if (config.checkUniqueness) {
                add(AssignmentUniquenessChecker)
            }
        }

        override val callCheckers: Set<FirCallChecker> = buildSet {
            if (config.checkLocality) {
                add(CallLocalityChecker)
                add(CallLocalityContractChecker)
            }

            if (config.checkUniqueness) {
                add(CallUniquenessChecker)
            }
        }

        override val functionCallCheckers: Set<FirFunctionCallChecker> = buildSet {
            if (config.checkUniqueness) {
                add(FunctionCallArgumentUniquenessCollisionChecker)
            }
        }

        override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = buildSet {
            if (config.checkLocality) {
                add(QualifiedAccessLocalityChecker)
                add(QualifiedAccessLocalityContractChecker)
            }

            if (config.checkUniqueness) {
                add(QualifiedAccessUniquenessChecker)
                add(QualifiedAccessArgumentUniquenessCollisionChecker)
            }
        }

        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = buildSet {
            // Specification blocks are not executed. Hence, they should be able to capture any variable regardless of
            // its locality.

            if (config.checkLocality) {
                add(PropertyAccessLocalityChecker.asSpecificationAware())
            }
        }

        override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = buildSet {
            if (config.checkLocality) {
                add(ReturnLocalityChecker)
                add(ReturnLocalityContractChecker)
            }

            if (config.checkUniqueness) {
                add(ReturnUniquenessChecker)
            }
        }

        override val throwExpressionCheckers: Set<FirThrowExpressionChecker> = buildSet {
            if (config.checkLocality) {
                add(ThrowLocalityChecker)
            }

            if (config.checkUniqueness) {
                add(ThrowUniquenessChecker)
            }
        }
    }

    override val typeCheckers: TypeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = buildSet {
            if (config.checkLocality) {
                add(TypeRefLocalityAttributeChecker)
            }

            if (config.checkUniqueness) {
                add(TypeRefUniquenessAttributeChecker)
            }
        }
    }
}

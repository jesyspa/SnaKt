/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirThrowExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.locality.contract.plugin.LocalityContractAdditionalCheckers
import org.jetbrains.kotlin.formver.locality.plugin.LocalityAdditionalCheckers
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessAdditionalCheckers

@JvmName("allAsSpecificationAwareDeclarationChecker")
fun <Declaration: FirDeclaration> Iterable<FirDeclarationChecker<Declaration>>.allAsSpecificationAware()
        : List<FirDeclarationChecker<Declaration>> =
    map { it.asSpecificationAware() }

@JvmName("allAsSpecificationAwareExpressionChecker")
fun <Expression: FirStatement> Iterable<FirExpressionChecker<Expression>>.allAsSpecificationAware()
        : List<FirExpressionChecker<Expression>> =
    map { it.asSpecificationAware() }

class PluginAdditionalCheckers(session: FirSession, config: PluginConfiguration) :
    FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(config: PluginConfiguration): Factory {
            return Factory { session -> PluginAdditionalCheckers(session, config) }
        }
    }

    private val uniquenessCheckers = UniquenessAdditionalCheckers(session)

    private val localityCheckers = LocalityAdditionalCheckers(session)

    private val localityContractCheckers = LocalityContractAdditionalCheckers(session)

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
            get() = buildSet {
                add(ViperPoweredDeclarationChecker(session, config))

                if (config.dumpUniquenessCFG) {
                    add(FunctionUniquenessStateRenderingChecker)
                }
            }

        override val functionCheckers: Set<FirFunctionChecker> = buildSet {
            addAll(uniquenessCheckers.declarationCheckers.functionCheckers.allAsSpecificationAware())
        }

        override val propertyCheckers: Set<FirPropertyChecker> = buildSet {
            addAll(uniquenessCheckers.declarationCheckers.propertyCheckers.allAsSpecificationAware())
            addAll(localityCheckers.declarationCheckers.propertyCheckers.allAsSpecificationAware())
            addAll(localityContractCheckers.declarationCheckers.propertyCheckers.allAsSpecificationAware())
        }

        override val valueParameterCheckers: Set<FirValueParameterChecker> = buildSet {
            addAll(localityCheckers.declarationCheckers.valueParameterCheckers.allAsSpecificationAware())
            addAll(localityContractCheckers.declarationCheckers.valueParameterCheckers.allAsSpecificationAware())
        }
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = buildSet {
            addAll(uniquenessCheckers.expressionCheckers.variableAssignmentCheckers.allAsSpecificationAware())
            addAll(localityCheckers.expressionCheckers.variableAssignmentCheckers.allAsSpecificationAware())
            addAll(localityContractCheckers.expressionCheckers.variableAssignmentCheckers.allAsSpecificationAware())
        }

        override val callCheckers: Set<FirCallChecker> = buildSet {
            addAll(uniquenessCheckers.expressionCheckers.callCheckers.allAsSpecificationAware())
            addAll(localityCheckers.expressionCheckers.callCheckers.allAsSpecificationAware())
            addAll(localityContractCheckers.expressionCheckers.callCheckers.allAsSpecificationAware())
        }

        override val functionCallCheckers: Set<FirFunctionCallChecker> = buildSet {
            addAll(uniquenessCheckers.expressionCheckers.functionCallCheckers.allAsSpecificationAware())
        }

        override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = buildSet {
            addAll(uniquenessCheckers.expressionCheckers.qualifiedAccessExpressionCheckers.allAsSpecificationAware())
            addAll(localityCheckers.expressionCheckers.qualifiedAccessExpressionCheckers.allAsSpecificationAware())
            addAll(localityContractCheckers.expressionCheckers.qualifiedAccessExpressionCheckers.allAsSpecificationAware())
        }

        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = buildSet {
            addAll(localityCheckers.expressionCheckers.propertyAccessExpressionCheckers.allAsSpecificationAware())
        }

        override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = buildSet {
            addAll(uniquenessCheckers.expressionCheckers.returnExpressionCheckers.allAsSpecificationAware())
            addAll(localityCheckers.expressionCheckers.returnExpressionCheckers.allAsSpecificationAware())
            addAll(localityContractCheckers.expressionCheckers.returnExpressionCheckers.allAsSpecificationAware())
        }

        override val throwExpressionCheckers: Set<FirThrowExpressionChecker> = buildSet {
            addAll(uniquenessCheckers.expressionCheckers.throwExpressionCheckers.allAsSpecificationAware())
            addAll(localityCheckers.expressionCheckers.throwExpressionCheckers.allAsSpecificationAware())
        }
    }

    override val typeCheckers: TypeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = buildSet {
            addAll(uniquenessCheckers.typeCheckers.resolvedTypeRefCheckers)
            addAll(localityCheckers.typeCheckers.resolvedTypeRefCheckers)
        }
    }
}

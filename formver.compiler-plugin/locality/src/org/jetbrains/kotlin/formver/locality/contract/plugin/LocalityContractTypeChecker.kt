/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.formver.locality.contract.plugin.LocalityContractErrors.CONTEXT_LOCALITY_CONTRACT_MISMATCH
import org.jetbrains.kotlin.formver.locality.contract.plugin.LocalityContractErrors.LOCALITY_CONTRACT_MISMATCH
import org.jetbrains.kotlin.formver.type.plugin.AssignmentTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.CallTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.PropertyTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.QualifiedAccessTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.ReturnTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.ValueParameterTypeFactChecker

val AssignmentLocalityContractChecker = AssignmentTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityContractJudgment,
    expressionTypeFactResolver = ExpressionLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)

val CallLocalityContractChecker = CallTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityContractJudgment,
    expressionTypeFactResolver = ExpressionLocalityContractResolver,
    callArgumentTypeFactsMapper = CallParametersLocalityContractResolver,
    argumentDiagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
    contextDiagnosticFactory = CONTEXT_LOCALITY_CONTRACT_MISMATCH
)

val PropertyLocalityContractChecker = PropertyTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityContractJudgment,
    expressionTypeFactResolver = ExpressionLocalityContractResolver,
    variableTypeFactResolver = VariableLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)

val QualifiedAccessLocalityContractChecker = QualifiedAccessTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityContractJudgment,
    expressionTypeFactResolver = ExpressionLocalityContractResolver,
    qualifiedAccessArgumentTypeFactMapper = QualifiedAccessArgumentLocalityContractsMapper,
    receiverDiagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
    contextArgumentDiagnosticFactory = CONTEXT_LOCALITY_CONTRACT_MISMATCH,
)

val ReturnLocalityContractChecker = ReturnTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityContractJudgment,
    expressionTypeFactResolver = ExpressionLocalityContractResolver,
    returnResultTypeFactResolver = ReturnResultLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)

val ValueParameterLocalityContractChecker = ValueParameterTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityContractJudgment,
    expressionTypeFactResolver = ExpressionLocalityContractResolver,
    parameterDeclaredTypeFactResolver = VariableLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)

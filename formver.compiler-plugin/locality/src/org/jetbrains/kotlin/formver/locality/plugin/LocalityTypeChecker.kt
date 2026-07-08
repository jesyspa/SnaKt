/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.CONTEXT_LOCALITY_MISMATCH
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_MISMATCH
import org.jetbrains.kotlin.formver.type.plugin.AssignmentTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.CallTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.PropertyTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.QualifiedAccessTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.ReturnTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.ThrowTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.ValueParameterTypeFactChecker

val AssignmentLocalityChecker = AssignmentTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityJudgment,
    expressionTypeFactResolver = ExpressionLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH,
)

val CallLocalityChecker = CallTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityJudgment,
    expressionTypeFactResolver = ExpressionLocalityResolver,
    callArgumentTypeFactsMapper = CallArgumentLocalitiesMapper,
    argumentDiagnosticFactory = LOCALITY_MISMATCH,
    contextDiagnosticFactory = CONTEXT_LOCALITY_MISMATCH,
)

val PropertyLocalityChecker = PropertyTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityJudgment,
    expressionTypeFactResolver = ExpressionLocalityResolver,
    variableTypeFactResolver = VariableLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)

val QualifiedAccessLocalityChecker = QualifiedAccessTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityJudgment,
    expressionTypeFactResolver = ExpressionLocalityResolver,
    qualifiedAccessArgumentTypeFactMapper = QualifiedAccessArgumentLocalitiesMapper,
    receiverDiagnosticFactory = LOCALITY_MISMATCH,
    contextArgumentDiagnosticFactory = CONTEXT_LOCALITY_MISMATCH,
)

val ReturnLocalityChecker = ReturnTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityJudgment,
    expressionTypeFactResolver = ExpressionLocalityResolver,
    returnResultTypeFactResolver = ReturnResultLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)

val ThrowLocalityChecker = ThrowTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityJudgment,
    expressionTypeFactResolver = ExpressionLocalityResolver,
    throwExceptionTypeFactResolver = ThrowExceptionLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)

val ValueParameterLocalityChecker = ValueParameterTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = LocalityJudgment,
    expressionTypeFactResolver = ExpressionLocalityResolver,
    parameterDeclaredTypeFactResolver = VariableLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)

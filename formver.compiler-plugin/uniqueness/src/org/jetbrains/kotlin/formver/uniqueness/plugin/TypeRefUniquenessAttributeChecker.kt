/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.formver.type.plugin.AttributeTargetJudgment
import org.jetbrains.kotlin.formver.type.plugin.TypeRefAttributeChecker

private object UniquenessAttributeTargetJudgment : AttributeTargetJudgment {
    context(context: CheckerContext)
    override fun accepts(parents: List<FirElement>): Boolean {
        val targetElement = parents.getOrNull(1) ?: return false

        return when (targetElement) {
            is FirValueParameter, is FirReceiverParameter, is FirProperty, is FirFunction -> true
            else -> targetElement.source?.kind is KtFakeSourceElementKind.ImplicitTypeArgument
        }
    }
}

val TypeRefUniquenessAttributeChecker = TypeRefAttributeChecker(
    kind = MppCheckerKind.Common,
    attributeClass = UniquenessAttribute::class,
    attributeTargetJudgment = UniquenessAttributeTargetJudgment,
    diagnosticFactory = UniquenessErrors.INVALID_UNIQUENESS_TYPE_TARGET,
)

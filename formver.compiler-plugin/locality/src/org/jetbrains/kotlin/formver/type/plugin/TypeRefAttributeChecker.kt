/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import kotlin.reflect.KClass

fun interface AttributeTargetJudgment {
    context(context: CheckerContext)
    fun accepts(parents: List<FirElement>): Boolean
}

class TypeRefAttributeChecker<Attribute : ConeAttribute<Attribute>>(
    kind: MppCheckerKind,
    private val attributeClass: KClass<Attribute>,
    private val attributeTargetJudgment: AttributeTargetJudgment,
    private val diagnosticFactory: KtDiagnosticFactory0,
) : FirResolvedTypeRefChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        if (typeRef.coneType.attributes[attributeClass] == null) return

        if (attributeTargetJudgment.accepts(context.containingElements.asReversed())) return

        reporter.reportOn(typeRef.source, diagnosticFactory)
    }
}

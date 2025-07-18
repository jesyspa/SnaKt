/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.uniqueness.UniqueChecker
import org.jetbrains.kotlin.formver.uniqueness.UniquenessCheckExceptionWrapper

class UniqueDeclarationChecker(private val session: FirSession, private val config: PluginConfiguration) :
    FirSimpleFunctionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        if (!config.checkUniqueness) return
        val errorCollector = ErrorCollector()
        try {
            val uniqueCheckerContext = UniqueChecker(session, config, errorCollector)
            declaration.accept(UniquenessCheckExceptionWrapper, uniqueCheckerContext)
        } catch (e: Exception) {
            val error = errorCollector.formatErrorWithInfos(e.message ?: "No message provided")
            reporter.reportOn(declaration.source, PluginErrors.UNIQUENESS_VIOLATION, error)
        }
    }
}
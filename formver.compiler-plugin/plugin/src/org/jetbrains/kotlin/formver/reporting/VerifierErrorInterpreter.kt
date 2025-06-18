/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.reporting

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.formver.ErrorStyle
import org.jetbrains.kotlin.formver.PluginErrors
import org.jetbrains.kotlin.formver.viper.errors.ConsistencyError
import org.jetbrains.kotlin.formver.viper.errors.VerificationError
import org.jetbrains.kotlin.formver.viper.errors.VerifierError

private fun VerificationError.formatByErrorStyle(errorStyle: ErrorStyle): List<FormattedError> = when (errorStyle) {
    ErrorStyle.USER_FRIENDLY -> listOf(formatUserFriendly() ?: DefaultError(this))
    ErrorStyle.ORIGINAL_VIPER -> listOf(DefaultError(this))
    ErrorStyle.BOTH -> listOfNotNull(formatUserFriendly(), DefaultError(this))
}

context(context: CheckerContext)
private fun DiagnosticReporter.reportVerificationError(
    source: KtSourceElement?,
    error: VerificationError,
    errorStyle: ErrorStyle,
) = error.formatByErrorStyle(errorStyle).forEach { it.report(source) }

context(context: CheckerContext)
private fun DiagnosticReporter.reportConsistencyError(source: KtSourceElement?, error: ConsistencyError) {
    val sourceIsFunctionDeclaration = source?.elementType?.let { it == KtNodeTypes.FUN } ?: false
    val positionStrategy = when (sourceIsFunctionDeclaration) {
        true -> SourceElementPositioningStrategies.DECLARATION_NAME
        false -> SourceElementPositioningStrategies.DEFAULT
    }
    reportOn(source, PluginErrors.INTERNAL_ERROR, error.msg, positioningStrategy = positionStrategy)
}

context(context: CheckerContext)
fun DiagnosticReporter.reportVerifierError(
    source: KtSourceElement?,
    error: VerifierError,
    errorStyle: ErrorStyle,
) = when (error) {
    is ConsistencyError -> reportConsistencyError(source, error)
    is VerificationError -> reportVerificationError(source, error, errorStyle)
}
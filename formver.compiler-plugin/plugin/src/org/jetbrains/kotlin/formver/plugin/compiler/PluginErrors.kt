/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.formver.common.VerificationErrorSeverity

object PluginErrors : KtDiagnosticsContainer() {
    val VIPER_TEXT by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXP_EMBEDDING by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INTERNAL_ERROR by error1<PsiElement, String>()
    val UNIQUENESS_VIOLATION by error1<PsiElement, String>()
    val UNIQUENESS_CFG by info1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ADT_VIOLATION by error1<PsiElement, String>()
    override fun getRendererFactory() = FormalVerificationPluginErrorMessages

    fun tags() = listOf(
        VIPER_TEXT.name,
        EXP_EMBEDDING.name,
        INTERNAL_ERROR.name,
        UNIQUENESS_VIOLATION.name,
        UNIQUENESS_CFG.name,
        ADT_VIOLATION.name
    )
}

/**
 * The factories a failed proof is reported through, at one severity.
 *
 * The Kotlin diagnostic API fixes a severity per factory, so a configurable
 * severity takes one set of factories per severity. Both sets use the same
 * property names, and a factory's name is its property name, so which set is in
 * use is invisible to anything naming a diagnostic: a `@Suppress`, an
 * `-Xwarning-level`, a test's expected tags.
 *
 * Every parameter is required, so a diagnostic added at one severity does not
 * compile until it is added at the other.
 */
data class VerificationDiagnostics(
    val conditionalEffectError: KtDiagnosticFactory2<String, String>,
    val viperVerificationError: KtDiagnosticFactory1<String>,
    val possibleIndexOutOfBound: KtDiagnosticFactory2<String, String>,
    val unexpectedReturnedValue: KtDiagnosticFactory1<String>,
    val invalidSublistRange: KtDiagnosticFactory2<String, String>,
) {
    companion object {
        private val warnings = VerificationDiagnostics(
            conditionalEffectError = VerificationErrors.CONDITIONAL_EFFECT_ERROR,
            viperVerificationError = VerificationErrors.VIPER_VERIFICATION_ERROR,
            possibleIndexOutOfBound = VerificationErrors.POSSIBLE_INDEX_OUT_OF_BOUND,
            unexpectedReturnedValue = VerificationErrors.UNEXPECTED_RETURNED_VALUE,
            invalidSublistRange = VerificationErrors.INVALID_SUBLIST_RANGE,
        )

        private val errors = VerificationDiagnostics(
            conditionalEffectError = StrictVerificationErrors.CONDITIONAL_EFFECT_ERROR,
            viperVerificationError = StrictVerificationErrors.VIPER_VERIFICATION_ERROR,
            possibleIndexOutOfBound = StrictVerificationErrors.POSSIBLE_INDEX_OUT_OF_BOUND,
            unexpectedReturnedValue = StrictVerificationErrors.UNEXPECTED_RETURNED_VALUE,
            invalidSublistRange = StrictVerificationErrors.INVALID_SUBLIST_RANGE,
        )

        fun of(severity: VerificationErrorSeverity): VerificationDiagnostics = when (severity) {
            VerificationErrorSeverity.WARNING -> warnings
            VerificationErrorSeverity.ERROR -> errors
        }
    }
}

object VerificationErrors : KtDiagnosticsContainer() {
    val CONDITIONAL_EFFECT_ERROR by warning2<PsiElement, String, String>()
    val VIPER_VERIFICATION_ERROR by warning1<PsiElement, String>()
    val POSSIBLE_INDEX_OUT_OF_BOUND by warning2<PsiElement, String, String>()
    val UNEXPECTED_RETURNED_VALUE by warning1<PsiElement, String>()
    val INVALID_SUBLIST_RANGE by warning2<PsiElement, String, String>()
    val CONSISTENCY by error1<PsiElement, String>()
    override fun getRendererFactory() = FormalVerificationPluginErrorMessages
    fun tags() = listOf(
        CONDITIONAL_EFFECT_ERROR.name,
        VIPER_VERIFICATION_ERROR.name,
        POSSIBLE_INDEX_OUT_OF_BOUND.name,
        UNEXPECTED_RETURNED_VALUE.name,
        INVALID_SUBLIST_RANGE.name,
        CONSISTENCY.name
    )
}

// Not registered as a diagnostic container: its renderers live in the factory
// VerificationErrors registers, which is enough for them to be found.
object StrictVerificationErrors : KtDiagnosticsContainer() {
    val CONDITIONAL_EFFECT_ERROR by error2<PsiElement, String, String>()
    val VIPER_VERIFICATION_ERROR by error1<PsiElement, String>()
    val POSSIBLE_INDEX_OUT_OF_BOUND by error2<PsiElement, String, String>()
    val UNEXPECTED_RETURNED_VALUE by error1<PsiElement, String>()
    val INVALID_SUBLIST_RANGE by error2<PsiElement, String, String>()
    override fun getRendererFactory() = FormalVerificationPluginErrorMessages
}

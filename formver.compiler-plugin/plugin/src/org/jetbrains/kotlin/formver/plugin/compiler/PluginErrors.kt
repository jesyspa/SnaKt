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
 * The diagnostics a failed proof is reported through.
 *
 * The Kotlin diagnostic API fixes a severity per factory, so reporting a failed
 * proof at a configurable severity takes one set of factories per severity.
 * The two sets carry the same diagnostic names, so which one is in use is
 * invisible to anything that refers to a diagnostic by name — a `@Suppress`, an
 * `-Xwarning-level`, a test's expected tags.
 */
// The property names are the diagnostic names: the delegates in the
// implementing containers derive one from the other.
@Suppress("VariableNaming")
sealed interface VerificationDiagnostics {
    val CONDITIONAL_EFFECT_ERROR: KtDiagnosticFactory2<String, String>
    val VIPER_VERIFICATION_ERROR: KtDiagnosticFactory1<String>
    val POSSIBLE_INDEX_OUT_OF_BOUND: KtDiagnosticFactory2<String, String>
    val UNEXPECTED_RETURNED_VALUE: KtDiagnosticFactory1<String>
    val INVALID_SUBLIST_RANGE: KtDiagnosticFactory2<String, String>

    companion object {
        fun of(severity: VerificationErrorSeverity): VerificationDiagnostics = when (severity) {
            VerificationErrorSeverity.WARNING -> VerificationErrors
            VerificationErrorSeverity.ERROR -> StrictVerificationErrors
        }
    }
}

object VerificationErrors : KtDiagnosticsContainer(), VerificationDiagnostics {
    override val CONDITIONAL_EFFECT_ERROR by warning2<PsiElement, String, String>()
    override val VIPER_VERIFICATION_ERROR by warning1<PsiElement, String>()
    override val POSSIBLE_INDEX_OUT_OF_BOUND by warning2<PsiElement, String, String>()
    override val UNEXPECTED_RETURNED_VALUE by warning1<PsiElement, String>()
    override val INVALID_SUBLIST_RANGE by warning2<PsiElement, String, String>()
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

object StrictVerificationErrors : KtDiagnosticsContainer(), VerificationDiagnostics {
    override val CONDITIONAL_EFFECT_ERROR by error2<PsiElement, String, String>()
    override val VIPER_VERIFICATION_ERROR by error1<PsiElement, String>()
    override val POSSIBLE_INDEX_OUT_OF_BOUND by error2<PsiElement, String, String>()
    override val UNEXPECTED_RETURNED_VALUE by error1<PsiElement, String>()
    override val INVALID_SUBLIST_RANGE by error2<PsiElement, String, String>()
    override fun getRendererFactory() = FormalVerificationPluginErrorMessages
}

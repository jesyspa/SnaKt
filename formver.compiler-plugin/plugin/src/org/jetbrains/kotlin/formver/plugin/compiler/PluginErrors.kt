/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*

object PluginErrors : KtDiagnosticsContainer() {
    val VIPER_TEXT by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXP_EMBEDDING by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INTERNAL_ERROR by error1<PsiElement, String>()
    val PROVER_NOT_FOUND by error1<PsiElement, String>()
    val UNIQUENESS_VIOLATION by error1<PsiElement, String>()
    val UNIQUENESS_CFG by info1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ADT_VIOLATION by error1<PsiElement, String>()
    override fun getRendererFactory() = FormalVerificationPluginErrorMessages

    fun tags() = listOf(
        VIPER_TEXT.name,
        EXP_EMBEDDING.name,
        INTERNAL_ERROR.name,
        PROVER_NOT_FOUND.name,
        UNIQUENESS_VIOLATION.name,
        UNIQUENESS_CFG.name,
        ADT_VIOLATION.name
    )
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

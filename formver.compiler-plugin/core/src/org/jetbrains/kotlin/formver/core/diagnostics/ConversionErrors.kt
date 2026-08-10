/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1

/**
 * Diagnostics emitted by the conversion pipeline (FIR -> ExpEmbedding -> Viper).
 *
 * Kept in `core` so conversion code can call `reporter.reportOn(source, ConversionErrors.X, msg)`
 * directly instead of buffering through an intermediate collector.
 */
object ConversionErrors : KtDiagnosticsContainer() {
    val PURITY_VIOLATION by error1<PsiElement, String>()
    val MINOR_INTERNAL_ERROR by error1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)

    /** A `preconditions`/`postconditions`/`loopInvariants` block sits in a position where the conversion never looks for it. */
    val IGNORED_SPEC_BLOCK by error1<PsiElement, String>()

    /**
     * Per-function summary fired by `ProgramConverter.validateAll` whenever a registered declaration's
     * pipeline (signature embedding, body conversion, or validation) produced any blocking errors.
     */
    val VERIFICATION_SKIPPED by error1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)

    override fun getRendererFactory() = ConversionErrorMessages
}

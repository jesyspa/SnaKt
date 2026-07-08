/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.formver.uniqueness.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.error3
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object UniquenessErrors : KtDiagnosticsContainer() {
    // Checking uniqueness type
    val UNIQUENESS_MISMATCH by error3<PsiElement, String, Uniqueness, Uniqueness>()
    val CONTEXT_UNIQUENESS_MISMATCH by error3<PsiElement, ConeKotlinType, Uniqueness, Uniqueness>()

    // Checking uniqueness consistency
    val ESCAPE_UNIQUENESS_INCONSISTENCY by error1<PsiElement, Path>()
    val CONTEXT_ESCAPE_UNIQUENESS_INCONSISTENCY by error2<PsiElement, ConeKotlinType, Path>()
    val EXIT_UNIQUENESS_INCONSISTENCY by error1<PsiElement, Path>()

    // Checking unique call arguments
    val INVALID_DUPLICATE_UNIQUE_ARGUMENT by error1<PsiElement, Path>()
    val INVALID_OVERLAPPING_UNIQUE_ARGUMENTS by error2<PsiElement, Path, Path>()

    // Checking unique property accesses
    val INVALID_MOVED_ACCESS by error0<PsiElement>()

    // Checking unique attributes
    val INVALID_UNIQUENESS_TYPE_TARGET by error0<PsiElement>()

    override fun getRendererFactory() = UniquenessErrorMessages
}

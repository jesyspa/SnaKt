/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.diagnostics

import org.jetbrains.kotlin.KtSourceElement

/**
 * Surface for emitting conversion diagnostics, narrower than `ProgramConversionContext` so that
 * subordinate code (purity checks, embedding builders, etc.) can depend on just diagnostic
 * reporting without pulling in the rest of the conversion machinery.
 */
interface ErrorCollectionContext {
    /** Report a purity violation at [source]. */
    fun reportPurityViolation(source: KtSourceElement?, msg: String)

    /** Report a minor internal error; the source is supplied by the implementation. */
    fun reportMinorInternalError(msg: String)

    /** Report a spec block (`preconditions`/`postconditions`/`loopInvariants`) at [source] that will be ignored. */
    fun reportIgnoredSpecBlock(source: KtSourceElement?, msg: String)
}

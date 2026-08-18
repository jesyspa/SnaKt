/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour

/**
 * Single entry point for reporting that the plugin does not support some construct the user
 * wrote (as opposed to an internal invariant violation, which should keep using
 * `SnaktInternalException`/`error` directly).
 *
 * Honors `config.behaviour`: in `THROW_EXCEPTION` mode this throws; in `ASSUME_UNREACHABLE`
 * mode it reports a minor internal error diagnostic and falls back to [onUnreachable], which
 * lets callers keep converting the rest of the program instead of aborting.
 */
inline fun <T> ProgramConversionContext.handleUnsupportedFeature(
    source: KtSourceElement?,
    msg: String,
    onUnreachable: () -> T,
): T = when (config.behaviour) {
    UnsupportedFeatureBehaviour.THROW_EXCEPTION -> throw SnaktInternalException(source, msg)
    UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
        reportMinorInternalError(msg)
        onUnreachable()
    }
}

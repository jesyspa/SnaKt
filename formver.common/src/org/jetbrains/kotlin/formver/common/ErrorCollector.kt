/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

import org.jetbrains.kotlin.KtSourceElement

/** Collector for some plugin errors.
 *
 * We currently are not consistent with what we report this way, vs through other channels.
 *
 * TODO: Replace this with some kind of more systematic approach to generating diagnostics.
 */
class ErrorCollector {
    private val minorErrors = mutableListOf<String>()
    private val purityErrors = mutableListOf<Pair<KtSourceElement, String>>()

    fun addMinorError(error: String) {
        minorErrors.add(error)
    }

    fun forEachMinorError(action: (String) -> Unit) {
        minorErrors.forEach(action)
    }

    fun addPurityError(position: KtSourceElement, msg: String) {
        purityErrors.add(Pair(position, msg))
    }

    fun forEachPurityError(action: (KtSourceElement, String) -> Unit) {
        purityErrors.forEach { (key, value) ->
            action(key, value)
        }
    }
}
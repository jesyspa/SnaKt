/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

/**
 * Severity at which a failed proof is reported.
 *
 * At [WARNING] a failed proof leaves the build green, so that adopting the plugin
 * cannot break an existing build; at [ERROR] it fails compilation like any other
 * compiler error.
 */
enum class VerificationErrorSeverity {
    WARNING,
    ERROR;

    companion object {
        @JvmStatic
        fun defaultBehaviour(): VerificationErrorSeverity = WARNING
    }
}

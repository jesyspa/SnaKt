/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

enum class LogLevel {
    ONLY_WARNINGS,
    SHORT_VIPER_DUMP,
    SHORT_VIPER_DUMP_WITH_PREDICATES,
    FULL_VIPER_DUMP;

    companion object {
        @JvmStatic
        fun defaultLogLevel(): LogLevel {
            return ONLY_WARNINGS
        }
    }
}


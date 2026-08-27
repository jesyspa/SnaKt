/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

/**
 * Trailer of source-position and diagnostic metadata carried by every Viper AST node.
 */
interface WithSilverMetadata {
    val pos: Position
    val info: Info
}

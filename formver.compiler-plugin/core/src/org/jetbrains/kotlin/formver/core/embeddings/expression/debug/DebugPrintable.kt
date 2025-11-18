/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression.debug

/**
 * Interface for objects that can be converted to a debug tree view.
 *
 * Debug output uses `debugMangled` for name resolution and does NOT require
 * name registration. This is intended for diagnostics, logging, and debugging only.
 */
interface DebugPrintable {
    val debugTreeView: TreeView
}
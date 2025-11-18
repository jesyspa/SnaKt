/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
const val SEPARATOR = "$"
interface SymbolicName {
    val mangledType: String?
        get() = null
    context(nameResolver: NameResolver)
    val mangledScope: String?
        get() = null
    context(nameResolver: NameResolver)
    val mangledBaseName: String
}

/**
 * Resolves this symbolic name using the provided name resolver context.
 *
 * For production code (toSilver conversion), use with SimpleNameResolver
 * after names have been registered via `program.registerAllNames()`.
 */
context(nameResolver: NameResolver)
val SymbolicName.mangled: String
    get() = nameResolver.resolve(this)

/**
 * Resolves this symbolic name for debug output without requiring registration.
 *
 * Use this for:
 * - Debug output and logging
 * - Error messages and diagnostics
 * - Any non-production string representation
 *
 * Do NOT use this for production `toSilver()` conversion - use `mangled` with
 * a registered SimpleNameResolver instead.
 */
val SymbolicName.debugMangled: String
    get() = DebugNameResolver().resolve(this)


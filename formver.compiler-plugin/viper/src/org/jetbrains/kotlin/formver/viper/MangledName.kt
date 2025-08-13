/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import org.jetbrains.kotlin.formver.viper.ast.Program
import viper.silver.ast.utility.Consistency
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */

interface MangledName {
    val mangledType: String?
        get() = null
    context(nameResolver: NameResolver)
    val mangledScope: String?
        get() = null
    context(nameResolver: NameResolver)
    val mangledBaseName: String
    context(nameResolver: NameResolver)
    fun registry(): Unit
}
context(nameResolver: NameResolver)
val MangledName.mangled: String
    get() = nameResolver.resolve(this)

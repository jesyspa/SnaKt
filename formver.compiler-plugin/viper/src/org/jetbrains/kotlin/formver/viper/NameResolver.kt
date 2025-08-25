package org.jetbrains.kotlin.formver.viper

import org.jetbrains.kotlin.formver.viper.MangledName

/**
 * Interface defining a strategy for converting a [ScopedKotlinName]
 * into an internal Viper identifier ([MangledName]).
 *
 * Multiple conversion strategies can be implemented and passed
 * to the `toViper(...)` function as needed.
 */

interface NameResolver {
    fun resolve(name: MangledName): String
    fun registry(name: MangledName)
}

class DebugNameResolver : NameResolver {
    override fun resolve(name: MangledName): String = listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)
    override fun registry(name: MangledName) {}
}
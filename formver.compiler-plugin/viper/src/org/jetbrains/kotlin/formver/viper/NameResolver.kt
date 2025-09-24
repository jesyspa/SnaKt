package org.jetbrains.kotlin.formver.viper

import org.jetbrains.kotlin.formver.viper.SymbolName

/**
 * Interface defining a strategy for converting a [ScopedKotlinName]
 * into an internal Viper identifier ([SymbolName]).
 *
 * Multiple conversion strategies can be implemented and passed
 * to the `toViper(...)` function as needed.
 */

interface NameResolver {
    fun resolve(name: SymbolName): String
    fun register(name: SymbolName)
}

class DebugNameResolver : NameResolver {
    override fun resolve(name: SymbolName): String = listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)
    override fun register(name: SymbolName) {}
}
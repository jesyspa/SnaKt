package org.jetbrains.kotlin.formver.viper

/**
 * Interface defining a strategy for converting a [ScopedKotlinName]
 * into an internal Viper identifier ([SymbolicName]).
 *
 * Multiple conversion strategies can be implemented and passed
 * to the `toViper(...)` function as needed.
 */

interface NameResolver {
    fun resolve(name: SymbolicName): String
    fun register(name: SymbolicName)
}
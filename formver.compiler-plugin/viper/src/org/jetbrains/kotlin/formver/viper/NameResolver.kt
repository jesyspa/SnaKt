package org.jetbrains.kotlin.formver.viper

/**
 * Interface for resolving symbolic names to their string representations.
 *
 * There are two main implementations:
 * 1. **SimpleNameResolver** (production): Requires registration before use.
 *    Names must be registered via `register()` before `toSilver()` conversion.
 * 2. **DebugNameResolver** (debug only): No registration needed.
 *    Used for debug output, error messages, and diagnostics.
 */
interface NameResolver {
    /**
     * Resolves a symbolic name to its string representation.
     * For production resolvers, names should be registered first via `register()`.
     */
    fun resolve(name: SymbolicName): String

    /**
     * Registers a name for later resolution.
     * Production resolvers use this to pre-compute and cache names.
     * Debug resolvers implement this as a no-op.
     */
    fun register(name: SymbolicName)
}

/**
 * Debug-only name resolver that resolves names on-demand without registration.
 *
 * This resolver is used for:
 * - Debug output and diagnostics
 * - Error messages
 * - Logging
 *
 * It does NOT require registration and should NOT be used for production
 * `toSilver()` conversion. For production conversion, use SimpleNameResolver.
 *
 * Use via the `debugMangled` extension property on SymbolicName.
 */
class DebugNameResolver : NameResolver {
    override fun resolve(name: SymbolicName): String = with(this) {
        listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)
    }

    override fun register(name: SymbolicName) {
        // No-op: debug resolver doesn't require registration
    }
}

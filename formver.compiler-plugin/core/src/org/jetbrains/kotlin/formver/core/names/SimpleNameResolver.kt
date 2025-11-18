package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SEPARATOR

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 *
 * This resolver caches resolved names to ensure consistency across the conversion process.
 *
 * Current strategy:
 *  1. Concatenate all non-null components (type, scope, baseName) with SEPARATOR.
 *  2. Cache resolved names for consistency across multiple resolve() calls.
 *
 * Names are registered during the registration phase before conversion to Silver,
 * ensuring all names are pre-computed and available during the toSilver conversion.
 *
 * Future strategy:
 *  1. Try to use a short name: <type>_<baseName>.
 *  2. If the short name is reserved or conflicts with an existing name of the same type, fall back to a long name:
 *     <type>_<scope>_<baseName>.
 *  3. Track used names to detect conflicts for future resolutions.
 */
class SimpleNameResolver : NameResolver {
    private val resolvedNames = mutableMapOf<SymbolicName, String>()

    override fun resolve(name: SymbolicName): String {
        // Return cached result if available
        resolvedNames[name]?.let { return it }

        // Build the resolved name by concatenating all components
        // Note: mangledScope and mangledBaseName use this resolver as context,
        // so nested symbolic names are automatically resolved recursively
        val result = listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)

        // Cache the result for future lookups
        resolvedNames[name] = result
        return result
    }

    override fun register(name: SymbolicName) {
        // Pre-compute and cache the resolved name
        resolve(name)
    }
}
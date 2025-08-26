package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SEPARATOR

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 *
 * Current strategy (simplified, will be extended later):
 *  1. Concatenate all non-null components (type, scope, baseName) with SEPARATOR.
 *
 * Future strategy:
 *  1. Try to use a short name: <type>_<baseName>.
 *  2. If the short name is reserved or conflicts with an existing name of the same type, fall back to a long name:
 *     <type>_<scope>_<baseName>.
 *  3. Track used names to detect conflicts for future resolutions.
 */
class SimpleNameResolver : NameResolver {
    override fun resolve(name: MangledName): String =
        listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)
    override fun registry(name: MangledName) {}
}
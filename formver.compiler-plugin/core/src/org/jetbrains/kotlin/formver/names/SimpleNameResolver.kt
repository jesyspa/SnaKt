package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.ast.Type
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Produces compact yet unique Viper identifiers.
 *
 * Strategy:
 *  1. Prefix based on entity kind: parameter → p_, local → l_, etc.
 *  2. Strip unsupported characters.
 *  3. Ensure uniqueness by appending _<index> when necessary.
 */
val reservedViper = setOf(
    "field", "method", "function", "result", "predicate",
    "domain", "axiom", "define", "requires", "ensures", "acc",
    "package", "unique", "derives"
)
class SimpleNameResolver : NameResolver {
    private var typesWithShortName: MutableSet<String?> = hashSetOf()
    private var typesToNames: MutableMap<String?, MutableSet<MangledName>> = hashMapOf()
    override fun resolve(name: MangledName): String {
        val type = name.mangledType
        val baseName = name.mangledBaseName

        var shortName = listOfNotNull(type, baseName).joinToString("_")
        if (reservedViper.contains(shortName)) shortName += "Value"

        if (typesWithShortName.contains(type)) {
            return shortName
        } else {
            val longName = listOfNotNull(type, name.mangledScope, baseName).joinToString("_")
            return longName
        }
    }
    override fun registry(name: MangledName) {
        val type = name.mangledType
        val baseName = name.mangledBaseName
        val newShortName = listOfNotNull(type, baseName).joinToString("_")

        val existingSet = typesToNames.getOrPut(type) {
            typesWithShortName.add(type)
            mutableSetOf()
        }
        val hasConflict = existingSet.any {
            it != name && (listOfNotNull(it.mangledType, it.mangledBaseName).joinToString("_") == newShortName)
        }

        if (hasConflict) {
            typesWithShortName.remove(type)
        }
        existingSet.add(name)
    }
}

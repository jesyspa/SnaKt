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
class SimpleNameResolver : NameResolver {
    private var names: MutableMap<String?, MutableSet<MangledName>> = mutableMapOf()
    private var types: MutableMap<String?, String> = mutableMapOf()
    override fun resolve(name: MangledName): String {
        //return listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString("_")
        val longName = listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString("_")

        val shortName = listOfNotNull(name.mangledType, name.mangledBaseName).joinToString("_")
        if (types[name.mangledType]=="short") {
            return shortName
        } else {
            return longName
        }
    }
    override fun registry(name: MangledName) {
        val key = name.mangledType
        val newShortName = listOfNotNull(name.mangledType, name.mangledBaseName).joinToString("_")

        val existingSet = names.getOrPut(key) {
            types[key] = "short"
            mutableSetOf()
        }
        val hasConflict = existingSet.any {
            it != name && (it.mangledType + "_" + it.mangledBaseName == newShortName)
        }

        if (hasConflict) {
            types[key] = "long"
        }

        existingSet.add(name)
    }
}

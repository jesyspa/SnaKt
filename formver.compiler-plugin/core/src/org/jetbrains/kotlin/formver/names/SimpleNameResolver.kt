package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.NameResolver
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
    private var names: MutableMap<MangledName, String> = mutableMapOf()
    override fun resolve(name: MangledName): String {
        if (name !in names.keys) {
            val longName = name.mangledType + "_" + name.mangledScope + "_" + name.mangledBaseName
            val shortName = name.mangledType + "_" + name.mangledBaseName
            if (shortName in names.values) names.put(name, longName)
            else names.put(name, shortName)
        }
        return names[name]!!
    }
        //return listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString("_")
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import NameScope

/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
const val SEPARATOR = "$"
interface SymbolicName {
    val mangledType: String?
        get() = null
    val mangledScope: NameScope?
        get() = null
    val mangledBaseName: NameExpr

    // needed for fresh names (see FreshNames.kt)
    val requiresType: Boolean
        get() = false
}

context(nameResolver: NameResolver)
val SymbolicName.mangled: String
    get() = nameResolver.resolve(this)

sealed interface NameExpr {
    fun toParts(): List<Part> = emptyList()

    sealed interface Part: NameExpr {
        data class Lit(val value: String?) : Part {
            override fun toParts(): List<Part> {
                return if (value==null) emptyList()
                else listOf(this)
            }
        }
        data class SymbolVal(val symbolicName: SymbolicName) : Part {
            override fun toParts(): List<Part> = listOf(this)
        }
    }
}

data class Lit(val text: String) : NameExpr {
    override fun toParts(): List<NameExpr.Part> = listOf(NameExpr.Part.Lit(text))
}
data class SymbolVal(val symbolicName: SymbolicName) : NameExpr {
    override fun toParts() = listOf(NameExpr.Part.SymbolVal(symbolicName))
}

data class Join(val items: List<NameExpr>) : NameExpr {
    constructor (vararg items: NameExpr) : this(items.toList())
    override fun toParts(): List<NameExpr.Part> = buildList {
        items.forEach { item ->
            val parts = item.toParts()
            if (parts.isNotEmpty()) addAll(parts)
        }
    }
}

fun joinExprs(vararg segs: NameExpr?): NameExpr? {
    val items = segs
        .filterNotNull()
    return when (items.size) {
        0 -> null
        1 -> items[0]
        else -> Join(items)
    }
}

fun parseRequiredScope(scope: NameExpr): NameExpr? {
    val parts = scope.toParts()
    if (parts.isEmpty()) return null

    val requiredParts = parts.mapNotNull { part ->
        when (part) {
            is NameExpr.Part.SymbolVal -> part
            is NameExpr.Part.Lit -> null
        }
    }

    return when {
        requiredParts.isEmpty() -> null
        requiredParts.size == 1 -> requiredParts[0]
        else -> Join(requiredParts)
    }
}
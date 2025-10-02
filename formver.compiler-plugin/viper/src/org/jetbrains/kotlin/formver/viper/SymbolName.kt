/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
const val SEPARATOR = "$"
interface SymbolName {
    val mangledType: String?
        get() = null
    val requiredScope: NameExpr?
        get() = null
    val fullScope: NameExpr?
        get() = null
    val mangledBaseName: NameExpr
    val requiresType: Boolean
        get() = false
}

context(nameResolver: NameResolver)
val SymbolName.mangled: String
    get() = nameResolver.resolve(this)

sealed interface NameExpr {
    fun toParts(): List<Part> = emptyList()

    sealed interface Part: NameExpr {
        data class Lit(val value: String?) : Part {
            override fun toParts(): List<Part> {
                return if (value.equals("")) emptyList()
                else listOf(this)
            }
        }
        data class SymbolVal(val SymbolName: SymbolName) : Part {
            override fun toParts(): List<Part> = listOf(this)
        }
    }
}

data class Lit(val text: String?) : NameExpr {
    override fun toParts(): List<NameExpr.Part> {
        return if (text.equals("") || text==null) emptyList()
        else listOf(NameExpr.Part.Lit(text))
    }
}
data class SymbolVal(val SymbolName: SymbolName) : NameExpr {
    override fun toParts() = listOf(NameExpr.Part.SymbolVal(SymbolName))
}

data class Join(val items: List<NameExpr>, val sep: String = SEPARATOR) : NameExpr {
    override fun toParts(): List<NameExpr.Part> {
        if (items.isEmpty()) return emptyList()
        val out = mutableListOf<NameExpr.Part>()
        items.mapNotNull { item ->
            item.toParts().takeIf { it.isNotEmpty() }
        }.forEachIndexed { i, parts ->
            out += parts
            if (i < items.count { !it.toParts().isEmpty() } - 1) {
                out += NameExpr.Part.Lit(sep)
            }
        }
        return out
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
        requiredParts.isEmpty() -> Lit("")
        requiredParts.size == 1 -> requiredParts[0]
        else -> Join(requiredParts, SEPARATOR)
    }
}
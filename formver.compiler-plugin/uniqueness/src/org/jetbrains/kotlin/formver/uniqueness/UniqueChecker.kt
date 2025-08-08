/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class UniqueChecker(
    override val session: FirSession,
    override val config: PluginConfiguration,
    override val errorCollector: ErrorCollector,
) : UniqueCheckerContext {
    @Suppress("SameParameterValue")
    private fun getAnnotationId(name: String): ClassId =
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")), Name.identifier(name))

    private val uniqueId: ClassId
        get() = getAnnotationId("Unique")

    /**
     * Caches the current uniqueness level for FIR symbols that can hold or produce values whose aliasing is tracked.
     *
     * Keys:
     *   Variable-like symbols:
     *   - local variables
     *   - value parameters
     *   - properties/fields (incl. backing fields)
     *
     * Values:
     * - The symbol’s current UniqueLevel. On first access it is initialized from the declaration’s annotations
     *   (e.g., @Unique → Unique; otherwise Shared).
     *
     * Rules:
     * - The checker may refine levels during analysis, typically relaxing Unique → Shared when uniqueness is broken
     *   (e.g., aliasing, copying) or replacing Unique → Top when the symbol is moved
     */
    private val uniquenessContext: MutableMap<LocalPath, UniqueLevel> = mutableMapOf()
    private val isPartiallyMoved: MutableMap<LocalPath, Boolean> = mutableMapOf()

    override fun resolveUniqueAnnotation(declaration: HasAnnotation): UniqueLevel {
        if (declaration.hasAnnotation(uniqueId, session)) {
            return UniqueLevel.Unique
        }
        return UniqueLevel.Shared
    }

    override fun getUniqueLevel(symbol: LocalPath): UniqueLevel {
        val level = uniquenessContext.getOrPut(symbol) {
            resolveUniqueAnnotation(symbol.callee)
        }
        return level
    }

    override fun assignUniqueLevel(symbol: LocalPath, level: UniqueLevel) {
        uniquenessContext[symbol] = level
    }

    override fun markPartiallyMoved(symbol: LocalPath, mark: Boolean) {
        isPartiallyMoved[symbol] = mark
    }

    override fun isPartiallyMoved(symbol: LocalPath): Boolean =
        isPartiallyMoved[symbol] ?: false
}
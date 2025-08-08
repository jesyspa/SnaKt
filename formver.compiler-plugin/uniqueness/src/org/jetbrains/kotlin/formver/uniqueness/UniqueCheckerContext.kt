/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.name.ClassId

sealed interface HasAnnotation {
    class Symbol(val symbol: FirBasedSymbol<*>) : HasAnnotation {
        override fun hasAnnotation(id: ClassId, session: FirSession) = symbol.hasAnnotation(id, session)
    }

    class AnnotationContainer(val container: FirAnnotationContainer) : HasAnnotation {
        override fun hasAnnotation(id: ClassId, session: FirSession) = container.hasAnnotation(id, session)
    }

    fun hasAnnotation(id: ClassId, session: FirSession): Boolean
}

class UniqueLevelEmbedding(
    private val uniqueContext: UniqueCheckerContext,
    private val symbol: FirBasedSymbol<*>,
) {
    var level: UniqueLevel
        get() = uniqueContext.getUniqueLevel(symbol)
        set(newLevel) = uniqueContext.assignUniqueLevel(symbol, newLevel)
}

interface UniqueCheckerContext {
    val config: PluginConfiguration
    val errorCollector: ErrorCollector
    val session: FirSession

    fun resolveUniqueAnnotation(declaration: HasAnnotation): UniqueLevel

    fun getUniqueLevel(symbol: FirBasedSymbol<*>): UniqueLevel
    fun assignUniqueLevel(symbol: FirBasedSymbol<*>, level: UniqueLevel)

    fun markPartiallyMoved(symbol: FirBasedSymbol<*>, mark: Boolean = true)
    fun isPartiallyMoved(symbol: FirBasedSymbol<*>): Boolean
}

fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirBasedSymbol<*>): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.Symbol(declaration))

fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirAnnotationContainer): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.AnnotationContainer(declaration))

fun UniqueCheckerContext.uniqueLevelOf(symbol: FirBasedSymbol<*>): UniqueLevelEmbedding =
    UniqueLevelEmbedding(this, symbol)
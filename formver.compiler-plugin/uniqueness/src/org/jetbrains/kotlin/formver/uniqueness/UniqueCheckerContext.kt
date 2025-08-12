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

interface UniqueCheckerContext {
    val config: PluginConfiguration
    val errorCollector: ErrorCollector
    val session: FirSession

    fun resolveUniqueAnnotation(declaration: HasAnnotation): UniqueLevel

    /**
     * Retrieves the [ContextTrie] node corresponding to the given path.
     * If the path does not exist in the current context, inserts the necessary nodes.
     *
     * @param path A list of [FirBasedSymbol] items representing a sequence of symbols forming a path in code (`x.y.z -> [local/x, A/y, B/z]`).
     *             Note that the first element in the path must be a local symbol.
     * @return The [ContextTrie] node at the end of the specified path.
     *         If intermediate nodes do not exist, they are created with unique levels extracted from annotations.
     */
    fun getOrPutPath(path: List<FirBasedSymbol<*>>): UniquePathContext
}

fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirBasedSymbol<*>): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.Symbol(declaration))

fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirAnnotationContainer): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.AnnotationContainer(declaration))
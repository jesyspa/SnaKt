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

private fun<T> List<T>.headTail(): Pair<T, List<T>> = first() to drop(1)

class ContextTrie(
    val parent: ContextTrie?, val children: MutableMap<FirBasedSymbol<*>, ContextTrie>, var level: UniqueLevel
) {
    context(context: UniqueCheckerContext) fun getOrPutPath(path: List<FirBasedSymbol<*>>): ContextTrie {
        if (path.isEmpty()) return this

        val (head, tail) = path.headTail()
        val node = children.getOrPut(head) {
            ContextTrie(this, mutableMapOf(), context.resolveUniqueAnnotation(head))
        }
        return node.getOrPutPath(tail)
    }

    fun subtreeLUB(): UniqueLevel = listOfNotNull(level, children.values.maxOfOrNull { it.subtreeLUB() }).max()
    fun pathLUB(): UniqueLevel = listOfNotNull(parent?.pathLUB(), level).max()
}

interface UniqueCheckerContext {
    val config: PluginConfiguration
    val errorCollector: ErrorCollector
    val session: FirSession

    fun resolveUniqueAnnotation(declaration: HasAnnotation): UniqueLevel

    fun getOrPutPath(path: List<FirBasedSymbol<*>>): ContextTrie
}

fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirBasedSymbol<*>): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.Symbol(declaration))

fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirAnnotationContainer): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.AnnotationContainer(declaration))
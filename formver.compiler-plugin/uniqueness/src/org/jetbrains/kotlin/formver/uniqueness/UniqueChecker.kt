/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
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

    private val borrowingId: ClassId
        get() = getAnnotationId("Borrowed")

    private val uniqueContext = ContextTrie(null, null, mutableMapOf(), UniqueLevel.Unique)

    override fun resolveUniqueAnnotation(declaration: HasAnnotation): UniqueLevel {
        if (declaration.hasAnnotation(uniqueId, session)) {
            return UniqueLevel.Unique
        }
        return UniqueLevel.Shared
    }

    override fun resolveBorrowingAnnotation(declaration: HasAnnotation): BorrowingLevel {
        if (declaration.hasAnnotation(borrowingId, session)) {
            return BorrowingLevel.Borrowed
        }
        return BorrowingLevel.Plain
    }

    override fun getOrPutPath(path: List<FirBasedSymbol<*>>): UniquePathContext {
        require(path.isNotEmpty()) { "Provided path is empty" }
        val head = path.first()
        require(head is FirValueParameterSymbol) { "Provided path does not start with a local variable" }

        return uniqueContext.getOrPutPath(path)
    }
}
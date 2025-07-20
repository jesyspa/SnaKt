/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration

interface UniqueCheckerContext {
    val config: PluginConfiguration
    val errorCollector: ErrorCollector
    val session: FirSession

    fun resolveUniqueAnnotation(declaration: FirBasedSymbol<*>): UniqueLevel
    fun resolveUniqueAnnotation(declaration: FirAnnotationContainer): UniqueLevel
}

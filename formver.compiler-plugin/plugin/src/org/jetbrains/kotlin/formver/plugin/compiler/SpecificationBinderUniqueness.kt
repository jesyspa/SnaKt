/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.core.conversion.extractPostconditionsReturnVar
import org.jetbrains.kotlin.formver.uniqueness.plugin.Uniqueness
import org.jetbrains.kotlin.formver.uniqueness.plugin.scopeUniqueness

/**
 * The uniqueness [this] takes from the function it specifies, when it binds the return value of a `postconditions`
 * block, and `null` otherwise.
 *
 * The binder stands for the value the function returns, so it is as unique as the declared return type. It cannot say
 * so itself: `@Unique` is rejected on the type argument of `postconditions`.
 */
context(context: CheckerContext)
fun FirVariableSymbol<*>.resolveSpecificationBinderUniqueness(): Uniqueness? {
    if (this !is FirValueParameterSymbol) return null

    val specifiedFunction = context.containingDeclarations
        .lastOrNull { it is FirNamedFunctionSymbol } as? FirNamedFunctionSymbol
        ?: return null
    if (specifiedFunction.extractPostconditionsReturnVar() != this) return null

    return specifiedFunction.resolvedReturnType.scopeUniqueness
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.formver.type.plugin.SymbolTypeFactResolver

/**
 * Resolves the uniqueness a symbol takes from the declaration it stands for, rather than from its own type.
 *
 * A binder of a specification block is such a symbol: its type is a type argument, which `@Unique` may not be written
 * on, so its uniqueness has to come from the signature the block specifies.
 */
fun interface InheritedUniquenessResolver {
    context(context: CheckerContext)
    fun resolveInheritedUniquenessOf(symbol: FirVariableSymbol<*>): Uniqueness?
}

/**
 * Session component that carries the [InheritedUniquenessResolver] declared uniqueness consults first.
 */
class SymbolUniquenessResolver(
    val inheritedUniquenessResolver: InheritedUniquenessResolver,
    session: FirSession
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(inheritedUniquenessResolver: InheritedUniquenessResolver = { null }): Factory {
            return Factory { session -> SymbolUniquenessResolver(inheritedUniquenessResolver, session) }
        }
    }
}

private val FirSession.symbolUniquenessResolver: SymbolUniquenessResolver
        by FirSession.sessionComponentAccessor()

fun FirReceiverParameterSymbol.resolveUniqueness(): Uniqueness =
    resolvedType.scopeUniqueness

context(context: CheckerContext)
fun FirVariableSymbol<*>.resolveUniqueness(): Uniqueness {
    if (resolvedReturnType is ConeErrorType) return Uniqueness.Shared

    context.session.symbolUniquenessResolver.inheritedUniquenessResolver
        .resolveInheritedUniquenessOf(this)?.let { return it }

    if (resolvedReturnTypeRef.source?.kind !is KtFakeSourceElementKind.ImplicitTypeRef) {
        return resolvedReturnType.scopeUniqueness
    }

    return resolvedInitializer?.resolveAccessUniqueness() ?: Uniqueness.Shared
}

object ParameterUniquenessResolver: SymbolTypeFactResolver<Uniqueness, FirValueParameterSymbol> {
    context(context: CheckerContext)
    override fun resolveTypeFactOf(symbol: FirValueParameterSymbol): Uniqueness =
        symbol.resolvedReturnType.parameterUniqueness
}

object VariableUniquenessResolver : SymbolTypeFactResolver<Uniqueness, FirVariableSymbol<*>> {
    context(context: CheckerContext)
    override fun resolveTypeFactOf(symbol: FirVariableSymbol<*>): Uniqueness =
        symbol.resolveUniqueness()
}

context(context: CheckerContext)
fun FirBasedSymbol<*>.resolveDeclaredUniqueness(): Uniqueness =
    when (this) {
        is FirVariableSymbol<*> -> resolveUniqueness()
        is FirReceiverParameterSymbol -> resolveUniqueness()
        else -> Uniqueness.Shared
    }

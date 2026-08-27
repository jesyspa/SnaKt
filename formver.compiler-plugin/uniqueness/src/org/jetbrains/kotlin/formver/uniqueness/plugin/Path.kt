package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.utils.memberDeclarationNameOrNull
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirThisOwnerSymbol
import kotlin.collections.joinToString

typealias Path = List<FirBasedSymbol<*>>

val PathRenderer = Renderer<Path> { path ->
    path.map { symbol ->
        symbol.memberDeclarationNameOrNull
            ?: (symbol as? FirReceiverParameterSymbol)?.let { "this" }
    }.joinToString(".")
}

context(context: CheckerContext)
fun Path.resolveDeclaredUniqueness(): Uniqueness =
    firstOrNull()?.resolveDeclaredUniqueness()?.join(subList(1, size).resolveDeclaredUniqueness())
        ?: Uniqueness.Unique

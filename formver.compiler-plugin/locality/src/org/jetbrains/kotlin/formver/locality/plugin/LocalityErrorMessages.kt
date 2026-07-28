/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

private val LocalityBoundarySymbolRenderer = Renderer<FirBasedSymbol<*>> { symbol ->
    val boundaryKind = when (symbol) {
        is FirAnonymousObjectSymbol ->
            return@Renderer "anonymous object"

        is FirAnonymousFunctionSymbol ->
            return@Renderer "anonymous function"

        is FirClassSymbol<*> ->
            if (symbol.classId.isLocal) "local class" else "class"

        is FirFunctionSymbol<*> ->
            if (symbol.callableId.isLocal) "local function" else "function"

        else -> "declaration"
    }

    val renderedName = FirDiagnosticRenderers.DECLARATION_NAME.render(symbol)

    "$boundaryKind '$renderedName'"
}

object LocalityErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap
            by KtDiagnosticFactoryToRendererMap("FormalVerificationLocality") { map ->
                map.put(
                    LocalityErrors.LOCALITY_MISMATCH,
                    "{0} locality mismatch: expected ''{1}'', actual ''{2}''.",
                    CommonRenderers.STRING,
                    LocalityRenderer,
                    LocalityRenderer,
                )
                map.put(
                    LocalityErrors.CONTEXT_LOCALITY_MISMATCH,
                    "Locality mismatch for context parameter of type ''{0}'': expected ''{1}'', actual ''{2}''.",
                    FirDiagnosticRenderers.RENDER_TYPE,
                    LocalityRenderer,
                    LocalityRenderer,
                )
                map.put(
                    LocalityErrors.INVALID_LOCALITY_CAPTURE,
                    "Unable to capture local ''{0}'' across {1}.",
                    FirDiagnosticRenderers.DECLARATION_NAME,
                    LocalityBoundarySymbolRenderer,
                )
                map.put(
                    LocalityErrors.INVALID_LOCALITY_TYPE_TARGET,
                    "Locality can only be specified on types of function parameters, extension receivers, or local variables.",
                )
            }
}

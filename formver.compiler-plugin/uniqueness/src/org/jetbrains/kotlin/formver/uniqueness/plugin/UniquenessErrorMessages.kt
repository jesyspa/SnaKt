/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers

object UniquenessErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap
            by KtDiagnosticFactoryToRendererMap("FormalVerificationUniqueness") { map ->
                map.put(
                    UniquenessErrors.UNIQUENESS_MISMATCH,
                    "{0} uniqueness mismatch: expected ''{1}'', actual ''{2}''.",
                    CommonRenderers.STRING,
                    UniquenessRenderer,
                    UniquenessRenderer,
                )
                map.put(
                    UniquenessErrors.CONTEXT_UNIQUENESS_MISMATCH,
                    "Uniqueness mismatch for context parameter of type ''{0}'': expected ''{1}'', actual ''{2}''.",
                    FirDiagnosticRenderers.RENDER_TYPE,
                    UniquenessRenderer,
                    UniquenessRenderer,
                )
                map.put(
                    UniquenessErrors.CONTEXT_ESCAPE_UNIQUENESS_INCONSISTENCY,
                    "Escaping context parameter of type ''{0}'' has moved field: ''{1}''.",
                    FirDiagnosticRenderers.RENDER_TYPE,
                    PathRenderer
                )
                map.put(
                    UniquenessErrors.INVALID_MOVED_ACCESS,
                    "Invalid access to moved reference."
                )
                map.put(
                    UniquenessErrors.INVALID_UNIQUENESS_TYPE_TARGET,
                    "Uniqueness can only be specified on values, properties, functions, and compatible type positions.",
                )
                map.put(
                    UniquenessErrors.ESCAPE_UNIQUENESS_INCONSISTENCY,
                    "Escaping value has moved field: ''{0}''.",
                    PathRenderer
                )
                map.put(
                    UniquenessErrors.EXIT_UNIQUENESS_INCONSISTENCY,
                    "Borrowed local value has moved field at exit: ''{0}''.",
                    PathRenderer
                )
                map.put(
                    UniquenessErrors.INVALID_DUPLICATE_UNIQUE_ARGUMENT,
                    "Invalid attempt to pass the same unique argument ''{0}'' twice.",
                    PathRenderer
                )
                map.put(
                    UniquenessErrors.INVALID_OVERLAPPING_UNIQUE_ARGUMENTS,
                    "Invalid attempt to pass unique argument ''{0}'' with overlapping unique argument ''{1}''.",
                    PathRenderer,
                    PathRenderer
                )
            }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.formver.common.VerificationErrorSeverity

private fun KtDiagnosticFactoryToRendererMap.putVerificationDiagnostics(diagnostics: VerificationDiagnostics) {
    put(
        diagnostics.viperVerificationError,
        "Viper verification error: {0}",
        CommonRenderers.STRING,
    )
    put(
        diagnostics.unexpectedReturnedValue,
        "Function may return a {0} value.",
        CommonRenderers.STRING,
    )
    put(
        diagnostics.conditionalEffectError,
        "Cannot verify that if {0} then {1}.",
        CommonRenderers.STRING,
        CommonRenderers.STRING,
    )
    put(
        diagnostics.possibleIndexOutOfBound,
        "Invalid index for {0}, the index may be {1}.",
        CommonRenderers.STRING,
        CommonRenderers.STRING,
    )
    put(
        diagnostics.invalidSublistRange,
        "Invalid sub-list range for {0}, the range may be {1}.",
        CommonRenderers.STRING,
        CommonRenderers.STRING,
    )
}

object FormalVerificationPluginErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("FormalVerification") { map ->
        map.put(
            PluginErrors.VIPER_TEXT,
            "Generated Viper text for {0}:\n{1}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        map.put(
            PluginErrors.EXP_EMBEDDING,
            "Generated ExpEmbedding for {0}:\n{1}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        VerificationErrorSeverity.entries.forEach { map.putVerificationDiagnostics(VerificationDiagnostics.of(it)) }
        map.put(
            VerificationErrors.CONSISTENCY,
            "Viper consistency error: {0}",
            CommonRenderers.STRING,
        )
        map.put(
            PluginErrors.INTERNAL_ERROR,
            "An internal error has occurred.\nDetails: {0}\nPlease report this at https://github.com/jesyspa/kotlin",
            CommonRenderers.STRING,
        )
        map.put(
            PluginErrors.UNIQUENESS_VIOLATION,
            "{0}",
            CommonRenderers.STRING,
        )

        map.put(
            PluginErrors.UNIQUENESS_CFG,
            "\n{0}",
            CommonRenderers.STRING,
        )
    }
}

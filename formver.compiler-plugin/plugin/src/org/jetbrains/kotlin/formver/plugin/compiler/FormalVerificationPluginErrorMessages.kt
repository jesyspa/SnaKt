/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

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
        map.put(
            VerificationErrors.VIPER_VERIFICATION_ERROR,
            "Viper verification error: {0}",
            CommonRenderers.STRING,
        )
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
            PluginErrors.PROVER_NOT_FOUND,
            "{0}",
            CommonRenderers.STRING,
        )
        map.put(
            VerificationErrors.UNEXPECTED_RETURNED_VALUE,
            "Function may return a {0} value.",
            CommonRenderers.STRING,
        )
        map.put(
            VerificationErrors.CONDITIONAL_EFFECT_ERROR,
            "Cannot verify that if {0} then {1}.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        map.put(
            VerificationErrors.POSSIBLE_INDEX_OUT_OF_BOUND,
            "Invalid index for {0}, the index may be {1}.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        map.put(
            VerificationErrors.INVALID_SUBLIST_RANGE,
            "Invalid sub-list range for {0}, the range may be {1}.",
            CommonRenderers.STRING,
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

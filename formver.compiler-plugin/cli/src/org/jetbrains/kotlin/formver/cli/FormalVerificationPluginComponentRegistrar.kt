/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.cli

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.formver.common.ErrorStyle
import org.jetbrains.kotlin.formver.plugin.compiler.FormalVerificationPluginExtensionRegistrar
import org.jetbrains.kotlin.formver.common.LogLevel
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.common.TargetsSelection
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour

@OptIn(ExperimentalCompilerApi::class)
class FormalVerificationPluginComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val logLevel = configuration.get(FormalVerificationConfigurationKeys.LOG_LEVEL, LogLevel.Companion.defaultLogLevel())
        val behaviour = configuration.get(
            FormalVerificationConfigurationKeys.UNSUPPORTED_FEATURE_BEHAVIOUR,
            UnsupportedFeatureBehaviour.Companion.defaultBehaviour()
        )
        val errorStyle = configuration.get(
            FormalVerificationConfigurationKeys.ERROR_STYLE,
            ErrorStyle.Companion.defaultBehaviour()
        )
        val conversionSelection = configuration.get(
            FormalVerificationConfigurationKeys.CONVERSION_TARGETS_SELECTION,
            TargetsSelection.Companion.defaultBehaviour()
        )
        val verificationSelection = configuration.get(
            FormalVerificationConfigurationKeys.VERIFICATION_TARGETS_SELECTION,
            TargetsSelection.Companion.defaultBehaviour()
        )
        // TODO: provide configuration to enable uniqueness checks
        val checkUniqueness = false
        val config = PluginConfiguration(
            logLevel, errorStyle, behaviour, conversionSelection, verificationSelection,
            checkUniqueness
        )
        FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))
    }
}

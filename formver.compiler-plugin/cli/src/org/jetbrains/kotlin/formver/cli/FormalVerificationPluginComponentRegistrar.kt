/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.cli

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.formver.common.*
import org.jetbrains.kotlin.formver.locality.plugin.LocalityExtensionRegistrar
import org.jetbrains.kotlin.formver.plugin.compiler.FormalVerificationPluginExtensionRegistrar
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessExtensionRegistrar

@OptIn(ExperimentalCompilerApi::class)
class FormalVerificationPluginComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = FormalVerificationPluginNames.PLUGIN_ID

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (!reportCompilerVersionSupported(configuration)) return

        val logLevel =
            configuration.get(FormalVerificationConfigurationKeys.LOG_LEVEL, LogLevel.Companion.defaultLogLevel())
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
        val checkUniqueness = configuration.get(FormalVerificationConfigurationKeys.CHECK_UNIQUENESS, false)
        val checkLocality = configuration.get(FormalVerificationConfigurationKeys.CHECK_LOCALITY, false)
        val dumpUniquenessCFG = configuration.get(FormalVerificationConfigurationKeys.DUMP_UNIQUENESS_CFG, false)
        val config = PluginConfiguration(
            logLevel, errorStyle, behaviour, conversionSelection, verificationSelection,
            checkLocality, checkUniqueness, dumpUniquenessCFG
        )
        FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))

        if (config.checkLocality) {
            FirExtensionRegistrarAdapter.registerExtension(LocalityExtensionRegistrar())
        }

        if (config.checkUniqueness) {
            FirExtensionRegistrarAdapter.registerExtension(UniquenessExtensionRegistrar())
        }
    }

    /**
     * Reports whether the compiler loading the plugin is one the plugin can bind to.
     *
     * The plugin is compiled against compiler internals that are not binary
     * compatible across Kotlin feature releases, and registering an extension
     * against a compiler that has moved on fails with a linkage error naming a
     * compiler class rather than the plugin. Refusing up front says which
     * versions the pairing needs instead.
     */
    private fun reportCompilerVersionSupported(configuration: CompilerConfiguration): Boolean {
        val running = KotlinCompilerVersion.getVersion() ?: return true
        if (featureRelease(running) <= featureRelease(BuildConfig.BUILT_AGAINST_KOTLIN_VERSION)) return true

        configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE).report(
            CompilerMessageSeverity.ERROR,
            "This build of SnaKt is compiled against Kotlin ${BuildConfig.BUILT_AGAINST_KOTLIN_VERSION} and " +
                    "cannot run on Kotlin $running. Compile with Kotlin " +
                    "${BuildConfig.BUILT_AGAINST_KOTLIN_VERSION} or older, or use a build of SnaKt made " +
                    "against Kotlin $running."
        )
        return false
    }

    /**
     * The feature release [version] belongs to, with the patch component dropped:
     * patch releases keep the internals the plugin binds to.
     */
    private fun featureRelease(version: String): KotlinVersion {
        val parts = version.split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return KotlinVersion(major, minor)
    }
}

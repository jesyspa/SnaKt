/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.runners


import org.jetbrains.kotlin.formver.common.services.DiagnosticsCollector
import org.jetbrains.kotlin.formver.common.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.formver.common.services.TagCollector
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors
import org.jetbrains.kotlin.formver.plugin.services.*
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.*

enum class TestMode {
    CHECK_CONVERSION, UPDATE, FULL
}

fun getTestMode(): TestMode {
    return when (System.getProperty("formver.testMode")) {
        "CHECK_CONVERSION" -> TestMode.CHECK_CONVERSION
        "UPDATE" -> TestMode.UPDATE
        "FULL" -> TestMode.FULL
        null -> TestMode.FULL
        else -> error("Unknown test mode: ${System.getProperty("formver.testMode")}")
    }
}

class ConversionDiagnosticsCollector(testServices: TestServices) : DiagnosticsCollector(testServices) {
    override val fileExtension: String = ".fir.diag.txt"
}

class VerificationDiagnosticsCollector(testServices: TestServices) : DiagnosticsCollector(testServices) {
    override val fileExtension: String = ".viper.diag.txt"
}

class ConversionTagCollector(testService: TestServices) : TagCollector(testService) {
    override val tagsToConsider: List<String> = PluginErrors.tags() + UniquenessErrors.tags()
}

class AllTagCollector(testService: TestServices) : TagCollector(testService)


val TestServices.conversionDiagnosticsCollector: ConversionDiagnosticsCollector by TestServices.testServiceAccessor()
val TestServices.verificationDiagnosticsCollector: VerificationDiagnosticsCollector by TestServices.testServiceAccessor()
val TestServices.conversionTagCollector: ConversionTagCollector by TestServices.testServiceAccessor()
val TestServices.allTagCollector: AllTagCollector by TestServices.testServiceAccessor()

/**
 * Runs tests with different modes based on the formver.testMode system property.
 * This test driver divides each test into two phases: conversion and verification.
 *
 * The conversion phase includes: uniqueness, conversion, purity, and viper consistency
 * The verification phase includes: viper verification
 *
 * The following modes exist:
 *
 * - CHECK_CONVERSION: runs only the conversion phase.
 * Use case: Check that code modification did not change the output of the conversion
 *
 * - UPDATE: runs the conversion and for those tests that changed, run the verification as well
 * Use case: After modifications check if the output is correct
 *
 * - FULL: runs all conversions and verification
 * Use case: Use before open PR and in the CI
 */
abstract class AbstractPhasedDiagnosticTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.FRONTEND
            +ENABLE_PLUGIN_PHASES
            +RENDER_DIAGNOSTICS_FULL_TEXT
            LANGUAGE with "+PropertyParamAnnotationDefaultTargetMode"
        }
        commonServicesConfigurationForCodegenAndDebugTest(FrontendKinds.FIR)

        facadeStep(::FirCliJvmFacade)
        handlersStep(FrontendKinds.FIR, compilationStage = CompilationStage.FIRST) {
            useHandlers(::AfterConversionHandler)
        }

        // These special services are used to divide the conversion and verification diagnostics/tags
        useAdditionalService(::ConversionDiagnosticsCollector)
        useAdditionalService(::VerificationDiagnosticsCollector)
        useAdditionalService(::ConversionTagCollector)
        useAdditionalService(::AllTagCollector)

        // This facade might verify the programs. This depends on the testMode and the result of the conversion check.
        facadeStep(::ViperProgramVerificationFacade)
        handlersStep(
            FrontendKinds.FIR, compilationStage = CompilationStage.FIRST
        ) {
            useHandlers(::ViperResultHandler)
        }

        configureFirParser(FirParser.LightTree)

        useAdditionalSourceProviders(::StdlibReplacementsProvider)

        useConfigurators(
            ::PluginAnnotationsProvider, ::ExtensionRegistrarConfigurator
        )
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}

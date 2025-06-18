/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.formver.*
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.ALWAYS_VALIDATE
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.FULL_VIPER_DUMP
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.REPLACE_STDLIB_EXTENSIONS
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.UNIQUE_CHECK_ONLY
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FormVerDirectives)

    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        if (FULL_VIPER_DUMP in module.directives && RENDER_PREDICATES in module.directives) {
            throw IllegalArgumentException("Directives FULL_VIPER_DUMP and RENDER_PREDICATES cannot be present in the same test file.")
        }

        val logLevel = when {
            FULL_VIPER_DUMP in module.directives -> LogLevel.FULL_VIPER_DUMP
            RENDER_PREDICATES in module.directives -> LogLevel.SHORT_VIPER_DUMP_WITH_PREDICATES
            else -> LogLevel.SHORT_VIPER_DUMP
        }
        val errorStyle = ErrorStyle.USER_FRIENDLY
        val verificationSelection = when {
            ALWAYS_VALIDATE in module.directives -> TargetsSelection.ALL_TARGETS
            NEVER_VALIDATE in module.directives || UNIQUE_CHECK_ONLY in module.directives -> TargetsSelection.NO_TARGETS
            else -> TargetsSelection.TARGETS_WITH_CONTRACT
        }
        val conversionSelection = when {
            UNIQUE_CHECK_ONLY in module.directives -> TargetsSelection.NO_TARGETS
            else -> TargetsSelection.ALL_TARGETS
        }
        val checkUniqueness = UNIQUE_CHECK_ONLY in module.directives
        val config = PluginConfiguration(
            logLevel,
            errorStyle,
            UnsupportedFeatureBehaviour.THROW_EXCEPTION,
            conversionSelection = conversionSelection,
            verificationSelection = verificationSelection,
            checkUniqueness = checkUniqueness
        )
        FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))
    }
}

object FormVerDirectives : SimpleDirectivesContainer() {
    val RENDER_PREDICATES by directive(
        description = "Outputs class predicates in diagnostic"
    )

    val FULL_VIPER_DUMP by directive(
        description = "Outputs the whole Viper code in diagnostic"
    )

    val ALWAYS_VALIDATE by directive(
        description = "Always validate functions"
    )

    val NEVER_VALIDATE by directive(
        description = "Never validate functions"
    )

    val UNIQUE_CHECK_ONLY by directive(
        description = "Do uniqueness checking"
    )

    val REPLACE_STDLIB_EXTENSIONS by directive(
        description = "Use replacements for stdlib functions like run with accessible bodies"
    )
}

class StdlibReplacementsProvider(testServices: TestServices, baseDir: String = ".") : AdditionalSourceProvider(testServices) {
    private val libraryPath = "$baseDir/formver.compiler-plugin/testData/stdlibReplacements.kt"

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile> =
        if (containsDirective(globalDirectives, module, REPLACE_STDLIB_EXTENSIONS))
            listOf(File(libraryPath).toTestFile())
        else emptyList()

}


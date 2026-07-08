/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.formver.common.*
import org.jetbrains.kotlin.formver.locality.plugin.LocalityExtensionRegistrar
import org.jetbrains.kotlin.formver.plugin.compiler.FormalVerificationPluginExtensionRegistrar
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.ALWAYS_VALIDATE
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.DUMP_UNIQUENESS_CFG
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.FULL_VIPER_DUMP
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.LOCALITY_CHECK_ONLY
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.REPLACE_STDLIB_EXTENSIONS
import org.jetbrains.kotlin.formver.plugin.services.FormVerDirectives.UNIQUE_CHECK_ONLY
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessExtensionRegistrar
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
        require(!(FULL_VIPER_DUMP in module.directives && RENDER_PREDICATES in module.directives)) {
            "Directives FULL_VIPER_DUMP and RENDER_PREDICATES cannot be present in the same test file."
        }

        val logLevel = when {
            FULL_VIPER_DUMP in module.directives -> LogLevel.FULL_VIPER_DUMP
            RENDER_PREDICATES in module.directives -> LogLevel.SHORT_VIPER_DUMP_WITH_PREDICATES
            else -> LogLevel.SHORT_VIPER_DUMP
        }
        val errorStyle = ErrorStyle.USER_FRIENDLY
        val conversionOnly = (System.getProperty("formver.conversionOnly")?.toBoolean() ?: false)
                || NEVER_VALIDATE in module.directives
        val uniquenessOnly = UNIQUE_CHECK_ONLY in module.directives
        val localityOnly = LOCALITY_CHECK_ONLY in module.directives
        val dumpUniquenessCFG = DUMP_UNIQUENESS_CFG in module.directives
        val verificationSelection = when {
            conversionOnly -> TargetsSelection.FORCE_DISABLE
            ALWAYS_VALIDATE in module.directives -> TargetsSelection.ALL_TARGETS
            uniquenessOnly || localityOnly -> TargetsSelection.NO_TARGETS
            else -> TargetsSelection.ALL_TARGETS
        }
        val conversionSelection = when {
            uniquenessOnly || localityOnly -> TargetsSelection.NO_TARGETS
            else -> TargetsSelection.ALL_TARGETS
        }
        val checkUniqueness = uniquenessOnly
        // Locality must run before uniqueness in tests.
        // UNIQUE_CHECK_ONLY enables both checkers (in this order), while LOCALITY_CHECK_ONLY keeps uniqueness off.
        val checkLocality = localityOnly || checkUniqueness
        val config = PluginConfiguration(
            logLevel,
            errorStyle,
            UnsupportedFeatureBehaviour.THROW_EXCEPTION,
            conversionSelection = conversionSelection,
            verificationSelection = verificationSelection,
            checkUniqueness = checkUniqueness,
            dumpUniquenessCFG = dumpUniquenessCFG,
            checkLocality = checkLocality,
        )
        FirExtensionRegistrarAdapter.registerExtension(FormalVerificationPluginExtensionRegistrar(config))
        if (config.checkLocality) {
            FirExtensionRegistrarAdapter.registerExtension(LocalityExtensionRegistrar())
        }
        if (config.checkUniqueness) {
            FirExtensionRegistrarAdapter.registerExtension(UniquenessExtensionRegistrar())
        }
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

    val UNIQUE_CHECK_ONLY by directive(
        description = "Do uniqueness checking (and run locality first)"
    )

    val LOCALITY_CHECK_ONLY by directive(
        description = "Do locality checking"
    )

    val DUMP_UNIQUENESS_CFG by directive(
        description = "dumps the CFG augmented with flow information"
    )

    val REPLACE_STDLIB_EXTENSIONS by directive(
        description = "Use replacements for stdlib functions like run with accessible bodies"
    )

    val NEVER_VALIDATE by directive(
        description = "Run in conversion-only mode: skip verification, keep consistency checking"
    )
}

class StdlibReplacementsProvider(testServices: TestServices, baseDir: String = ".") :
    AdditionalSourceProvider(testServices) {
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

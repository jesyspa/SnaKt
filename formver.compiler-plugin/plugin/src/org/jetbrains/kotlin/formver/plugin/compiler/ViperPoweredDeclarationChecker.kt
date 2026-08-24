/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.formver.common.LogLevel
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.TargetsSelection
import org.jetbrains.kotlin.formver.core.conversion.ProgramConverter
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.core.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.core.shouldVerify
import org.jetbrains.kotlin.formver.core.viperProgram
import org.jetbrains.kotlin.formver.plugin.compiler.reporting.reportVerifierError
import org.jetbrains.kotlin.formver.viper.SiliconFrontend
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.ast.registerAllNames
import org.jetbrains.kotlin.formver.viper.ast.unwrapOr
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val FirContractDescriptionOwner.hasContract: Boolean
    get() = when (val description = contractDescription) {
        is FirResolvedContractDescription -> description.effects.isNotEmpty()
        else -> false
    }

private fun TargetsSelection.applicable(declaration: FirSimpleFunction): Boolean = when (this) {
    TargetsSelection.ALL_TARGETS -> true
    TargetsSelection.TARGETS_WITH_CONTRACT -> declaration.hasContract
    TargetsSelection.NO_TARGETS -> false
    TargetsSelection.FORCE_DISABLE -> false
}

class ViperPoweredDeclarationChecker(private val session: FirSession, private val config: PluginConfiguration) :
    FirSimpleFunctionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        val inTestRun = System.getProperty("formver.testRun").toBoolean()
        if (!config.shouldConvert(declaration)) return
        try {
            val programConversionContext = ProgramConverter(session, config, context, reporter)
            programConversionContext.register(declaration)
            programConversionContext.validateAll()

            if (shouldDumpExpEmbeddings(declaration)) {
                with(SimpleNameResolver()) {
                    for ((name, embedding) in programConversionContext.debugExpEmbeddings) {
                        reporter.reportOn(
                            declaration.source,
                            PluginErrors.EXP_EMBEDDING,
                            name.mangled,
                            embedding.debugTreeView.print()
                        )
                    }
                }
            }

            if (programConversionContext.hadConversionError) return
            programConversionContext.linearizeAll()
            val program = programConversionContext.buildProgram()

            with(programConversionContext.nameResolver) {
                program.registerAllNames()
            }
            programConversionContext.nameResolver.resolve()

            getProgramForLogging(program)?.let {
                reporter.reportOn(
                    declaration.source,
                    PluginErrors.VIPER_TEXT,
                    declaration.name.asString(),
                    with(programConversionContext.nameResolver) { it.toDebugOutput() }
                )
            }

            val viperProgram = with(programConversionContext.nameResolver) { program.toSilver() }


            if (inTestRun) {
                declaration.viperProgram = viperProgram
                declaration.shouldVerify = config.shouldVerify(declaration)
            }


            if (!inTestRun) {
                // If we are in a test, then the verification happens later.

                val onFailure = { err: VerifierError ->
                    val source = err.position.unwrapOr { declaration.source }
                    reporter.reportVerifierError(source, err, config.errorStyle)
                }

                val verifier = SiliconFrontend(emptyList())
                verifier.use { it.verify(viperProgram, onFailure) }
            }

        } catch (e: SnaktInternalException) {
            reporter.reportOn(e.source, PluginErrors.INTERNAL_ERROR, e.message)
        } catch (e: Exception) {
            val error = e.message ?: "No message provided"
            reporter.reportOn(declaration.source, PluginErrors.INTERNAL_ERROR, error)
        }
    }

    private fun getProgramForLogging(program: Program): Program? = when (config.logLevel) {
        LogLevel.ONLY_WARNINGS -> null
        LogLevel.SHORT_VIPER_DUMP -> program.toShort().withoutPredicates()
        LogLevel.SHORT_VIPER_DUMP_WITH_PREDICATES -> program.toShort()
        LogLevel.FULL_VIPER_DUMP -> program
    }

    private fun getAnnotationId(name: String): ClassId =
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")), Name.identifier(name))

    private val neverConvertId: ClassId = getAnnotationId("NeverConvert")
    private val neverVerifyId: ClassId = getAnnotationId("NeverVerify")
    private val alwaysVerifyId: ClassId = getAnnotationId("AlwaysVerify")
    private val dumpExpEmbeddingsId: ClassId = getAnnotationId("DumpExpEmbeddings")

    private fun PluginConfiguration.shouldConvert(declaration: FirSimpleFunction): Boolean = when {
        // Prevent compiler-derived or library functions from being verified
        declaration.origin != FirDeclarationOrigin.Source -> false
        declaration.hasAnnotation(neverConvertId, session) -> false
        else -> conversionSelection.applicable(declaration)
    }

    private fun PluginConfiguration.shouldVerify(declaration: FirSimpleFunction): Boolean = when {
        declaration.hasAnnotation(neverConvertId, session) -> false
        declaration.hasAnnotation(neverVerifyId, session) -> false
        declaration.hasAnnotation(alwaysVerifyId, session) -> true
        else -> verificationSelection.applicable(declaration)
    }

    private fun shouldDumpExpEmbeddings(declaration: FirSimpleFunction): Boolean =
        declaration.hasAnnotation(dumpExpEmbeddingsId, session)
}

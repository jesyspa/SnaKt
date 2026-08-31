package org.jetbrains.kotlin.formver.common.services

import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoRenderer
import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithoutSource
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaInfo
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirMetaInfoUtils
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.*

fun KtDiagnostic.metaInfos(): List<FirDiagnosticCodeMetaInfo> {
    val ranges = when (this) {
        is KtDiagnosticWithSource -> textRanges
        is KtDiagnosticWithoutSource -> listOf(firstRange)
    }
    return ranges.map { range ->
        val metaInfo = FirDiagnosticCodeMetaInfo(this, FirMetaInfoUtils.renderDiagnosticNoArgs, range)
        metaInfo
    }
}


/**
 * Collects and asserts the tags in the test files.
 *
 * It is possible to ignore some tags when comparing. For this [tagsToConsider] must be overwritten by a list of tags that
 * should be considered for comparison.
 */
abstract class TagCollector(
    val testServices: TestServices,
) : TestService {

    open val tagsToConsider: List<String>?
        get() = null

    private var reportedInfos: MutableMap<TestFile, MutableList<CodeMetaInfo>> = mutableMapOf()
    private lateinit var existingInfos: Map<TestFile, List<CodeMetaInfo>>

    private fun reportMetaInfos(file: TestFile, codeMetaInfos: List<CodeMetaInfo>) {
        val filteredInfos = codeMetaInfos.filter { tagsToConsider == null || it.tag in tagsToConsider!! }
        val infos = reportedInfos.getOrPut(file) { mutableListOf() }
        infos += filteredInfos
    }

    fun parseExistingMetadataInfosFromAllSources() {
        existingInfos = buildMap {
            for (file in testServices.moduleStructure.modules.flatMap { it.files }) {
                put(file, CodeMetaInfoParser.getCodeMetaInfoFromText(file.originalContent))
            }
        }
    }

    fun reportDiagnostics(file: TestFile, codeMetaInfos: List<KtDiagnostic>) {
        reportMetaInfos(file, codeMetaInfos.flatMap { it.metaInfos() })
    }

    private fun renderText(
        metaInfo: Map<TestFile, List<CodeMetaInfo>>,
        sourceText: (testFile: TestFile) -> String
    ): String {
        val moduleStructure = testServices.moduleStructure
        val builder = StringBuilder()
        // In SplittingModuleTransformerForBoxTests, files may have been assigned to modules in a different order than declared in the original source file.
        // So, to reconstruct the original source file it needs not per-module traversal, but in original file order.
        val filesInOriginalOrder = moduleStructure.modules
            .flatMap { module -> module.files.map { file -> module to file } }
            .sortedBy { (_, file) -> file.startLineNumberInOriginalFile }
        for ((module, file) in filesInOriginalOrder) {
            if (!file.isAdditional) {
                val codeMetaInfos = metaInfo.getValue(file)
                val fileBuilder = StringBuilder()
                val source = sourceText(file)
                CodeMetaInfoRenderer.renderTagsToText(
                    fileBuilder,
                    codeMetaInfos,
                    source
                )
                val reverseTransformers =
                    testServices.sourceFileProvider.preprocessors.filterIsInstance<ReversibleSourceFilePreprocessor>()
                val initialFileContent = fileBuilder.stripAdditionalEmptyLines(file).toString()
                val actualFileContent =
                    reverseTransformers.foldRight(initialFileContent) { transformer, source ->
                        transformer.revert(
                            file,
                            source
                        )
                    }
                builder.append(actualFileContent)
            }
        }
        val actualText = builder.toString()
        return actualText
    }

    fun sourceFileWithoutTags(file: TestFile): String {
        val content = testServices.sourceFileProvider.getContentOfSourceFile(file)
        return clearTextFromDiagnosticMarkup(content)
    }

    fun assertEqual() {
        if (tagsToConsider == null) {
            assertFileEqual()
        } else {
            assertFileEqualFilteredForTags()
        }
    }

    /**
     * Asserts equality of the tags and the golden file
     */
    fun assertFileEqual() {
        testServices.assertions.assertEqualsToFile(
            testServices.moduleStructure.originalTestDataFiles.single(),
            renderText(reportedInfos, testServices.sourceFileProvider::getContentOfSourceFile)
        )
    }

    /**
     * The expected side of a filtered comparison comes from parsing markers back out of the test file, and a marker
     * names a tag and a range and nothing else: it cannot say that one diagnostic fired twice at one position. The
     * reported side is matched to that by collapsing such repeats. The message of each individual report is still
     * checked against the `.diag.txt` golden.
     */
    private fun deduplicatedReportedInfos(): Map<TestFile, List<CodeMetaInfo>> =
        reportedInfos.mapValues { (_, infos) -> infos.distinctBy { Triple(it.tag, it.start, it.end) } }

    /**
     * Renders the expected output but ignores tags that are not considered for comparison
     */
    private fun expectedFileFilteredForTags(): String {
        require(tagsToConsider != null) { "tagsToConsider must be set" }
        parseExistingMetadataInfosFromAllSources()
        val existingInfosWithoutVerification = existingInfos.mapValues {
            it.value.filter { info -> info.tag in tagsToConsider!! }
        }

        return renderText(existingInfosWithoutVerification, this::sourceFileWithoutTags)
    }

    /**
     * Asserts equality of the tags to consider and the golden file
     */
    fun assertFileEqualFilteredForTags() {
        val expectedOutput = expectedFileFilteredForTags()
        val actualOutput = renderText(deduplicatedReportedInfos(), testServices.sourceFileProvider::getContentOfSourceFile)
        testServices.assertions.assertEquals(expectedOutput, actualOutput) {
            "Actual tags differ from golden file"
        }
    }

    private fun StringBuilder.stripAdditionalEmptyLines(file: TestFile): CharSequence {
        return if (file.startLineNumberInOriginalFile != 0) {
            this.removePrefix((1..file.startLineNumberInOriginalFile).joinToString(separator = "") { "\n" })
        } else {
            this.toString()
        }
    }
}

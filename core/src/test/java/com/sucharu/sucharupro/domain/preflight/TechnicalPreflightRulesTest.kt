package com.sucharu.sucharupro.domain.preflight

import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.preflight.rules.*
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TechnicalPreflightRulesTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP02"
    private val artworkId = "ARTWORK-TECH-01"

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()

        // Register Step 02 Technical Rules
        registry.registerRule(FileExistenceRule())
        registry.registerRule(FileNonEmptyRule())
        registry.registerRule(FileFormatSupportedRule())
        registry.registerRule(FileFormatMatchRule())
        registry.registerRule(FileSignatureValidRule())
        registry.registerRule(DocumentParseableRule())
        registry.registerRule(DocumentPageCountRule())
        registry.registerRule(ImageStructureReadableRule())

        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_fileExistenceRule_missingFile_returnsError() = runBlocking {
        val rule = FileExistenceRule()

        // Valid File
        val validContext = PreflightExecutionContext(tenantId = tenantId, artworkId = artworkId)
        val validRes = rule.execute(validContext)
        assertEquals(PreflightExecutionResult.PASS, validRes.result)

        // Missing File
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val missingRes = rule.execute(missingContext)
        assertEquals(PreflightExecutionResult.ERROR, missingRes.result)
        assertEquals("RULE_001_FILE_EXISTS", missingRes.findings.first().ruleCode)
    }

    @Test
    fun test02_fileNonEmptyRule_emptyFile_returnsError() = runBlocking {
        val rule = FileNonEmptyRule()

        val emptyContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileSize" to 0L)
        )
        val emptyRes = rule.execute(emptyContext)
        assertEquals(PreflightExecutionResult.ERROR, emptyRes.result)
        assertEquals("Artwork file is zero bytes (empty).", emptyRes.findings.first().message)
    }

    @Test
    fun test03_fileFormatSupportedRule_unsupportedFormat_returnsError() = runBlocking {
        val rule = FileFormatSupportedRule()

        // Supported PDF
        val pdfContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("format" to "PDF")
        )
        assertEquals(PreflightExecutionResult.PASS, rule.execute(pdfContext).result)

        // Unsupported EXE
        val exeContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("format" to "EXE")
        )
        val exeRes = rule.execute(exeContext)
        assertEquals(PreflightExecutionResult.ERROR, exeRes.result)
        assertTrue(exeRes.findings.first().message.contains("not supported"))
    }

    @Test
    fun test04_fileFormatMatchRule_mismatchedExtension_returnsWarning() = runBlocking {
        val rule = FileFormatMatchRule()

        val mismatchedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("extension" to "pdf", "mimeType" to "image/png")
        )
        assertTrue(rule.isApplicable(mismatchedContext))

        val res = rule.execute(mismatchedContext)
        assertEquals(PreflightExecutionResult.WARNING, res.result)
        assertTrue(res.findings.first().message.contains("does not match declared MIME type"))
    }

    @Test
    fun test05_fileSignatureValidRule_corruptHeader_returnsError() = runBlocking {
        val rule = FileSignatureValidRule()

        val corruptContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("format" to "PDF", "magicSignature" to "CORRUPT_HEADER_BYTES")
        )
        assertTrue(rule.isApplicable(corruptContext))

        val res = rule.execute(corruptContext)
        assertEquals(PreflightExecutionResult.ERROR, res.result)
        assertTrue(res.findings.first().message.contains("does not match declared format"))
    }

    @Test
    fun test06_documentParseableRule_corruptedDocument_returnsError() = runBlocking {
        val rule = DocumentParseableRule()

        val corruptContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("isCorrupted" to true, "parseError" to "PDF cross-reference table corrupted")
        )
        val res = rule.execute(corruptContext)
        assertEquals(PreflightExecutionResult.ERROR, res.result)
        assertTrue(res.findings.first().message.contains("unparseable or corrupted"))
    }

    @Test
    fun test07_documentPageCountRule_extractsPageCount() = runBlocking {
        val rule = DocumentPageCountRule()

        val validDoc = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("format" to "PDF", "pageCount" to 16)
        )
        val res = rule.execute(validDoc)
        assertEquals(PreflightExecutionResult.PASS, res.result)
        assertEquals("16", res.findings.first().actualValue)
    }

    @Test
    fun test08_imageStructureReadableRule_extractsPixelDimensions() = runBlocking {
        val rule = ImageStructureReadableRule()

        val pngContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("format" to "PNG", "widthPx" to 1920, "heightPx" to 1080)
        )
        assertTrue(rule.isApplicable(pngContext))

        val res = rule.execute(pngContext)
        assertEquals(PreflightExecutionResult.PASS, res.result)
        assertEquals("1920x1080 px", res.findings.first().actualValue)
    }

    @Test
    fun test09_fullPreflightRun_validPdfArtwork_returnsPass() = runBlocking {
        val validPdfContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            jobId = "JOB-001",
            artworkMetadataMap = mapOf(
                "fileSize" to 2048500L,
                "format" to "PDF",
                "extension" to "pdf",
                "mimeType" to "application/pdf",
                "magicSignature" to "%PDF-1.7",
                "pageCount" to 8
            )
        )

        val result = service.runPreflight(validPdfContext, "STAFF-PREPRESS")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data

        assertEquals(PreflightRunStatus.COMPLETED, run.status)
        assertEquals(PreflightOverallResult.PASS, run.overallResult)

        val findingsRes = service.listFindingsForRun(tenantId, run.preflightRunId)
        assertTrue(findingsRes is DomainResult.Success)
        val findings = (findingsRes as DomainResult.Success).data
        // Expect INFO finding for page count
        assertTrue(findings.none { it.severity == PreflightRuleSeverity.ERROR })
    }

    @Test
    fun test10_canonicalProductionWorkflow_regressionCheck() {
        val expectedSequence = listOf(
            ProductionStageType.DESIGN,
            ProductionStageType.APPROVAL,
            ProductionStageType.QC,
            ProductionStageType.ITEM_APPROVAL,
            ProductionStageType.CTP,
            ProductionStageType.PRINTING,
            ProductionStageType.LAMINATION,
            ProductionStageType.FOLDING,
            ProductionStageType.BINDING,
            ProductionStageType.FINAL_QC,
            ProductionStageType.PACKAGING,
            ProductionStageType.READY,
            ProductionStageType.DELIVERED
        )
        assertEquals(13, ProductionStageType.entries.size)
        assertEquals(expectedSequence, ProductionStageType.entries)
    }
}

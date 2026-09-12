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
import java.math.BigDecimal

class SpecificationComplianceRulesTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP03"
    private val artworkId = "ARTWORK-SPEC-01"

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()

        // Register Step 02 & Step 03 Rules
        registry.registerRule(FileExistenceRule())
        registry.registerRule(FileNonEmptyRule())
        registry.registerRule(FileFormatSupportedRule())
        registry.registerRule(FileFormatMatchRule())
        registry.registerRule(FileSignatureValidRule())
        registry.registerRule(DocumentParseableRule())
        registry.registerRule(DocumentPageCountRule())
        registry.registerRule(ImageStructureReadableRule())

        registry.registerRule(SpecPageCountMatchRule())
        registry.registerRule(SpecDocumentSizeMatchRule())
        registry.registerRule(SpecOrientationMatchRule())
        registry.registerRule(SpecFormatMatchRule())
        registry.registerRule(SpecDocumentTypeMatchRule())

        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_specPageCountMatchRule_matchingAndMismatchedPageCount() = runBlocking {
        val rule = SpecPageCountMatchRule()

        // Match: Expected 64, Actual 64
        val matchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredPageCount" to 64),
            artworkMetadataMap = mapOf("pageCount" to 64)
        )
        assertEquals(PreflightExecutionResult.PASS, rule.execute(matchContext).result)

        // Mismatch: Expected 64, Actual 60
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredPageCount" to 64),
            artworkMetadataMap = mapOf("pageCount" to 60)
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("60"))
        assertTrue(mismatchRes.findings.first().message.contains("64"))
    }

    @Test
    fun test02_specDocumentSizeMatchRule_finishedDimensionsTolerance() = runBlocking {
        val rule = SpecDocumentSizeMatchRule()

        // Match: Finished size A4 (210x297 mm), Actual (210.2 x 296.9 mm) within ±1.0 mm
        val matchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("finishedWidthMm" to "210.00", "finishedHeightMm" to "297.00"),
            artworkMetadataMap = mapOf("widthMm" to BigDecimal("210.20"), "heightMm" to BigDecimal("296.90"))
        )
        assertEquals(PreflightExecutionResult.PASS, rule.execute(matchContext).result)

        // Mismatch: Finished size A4 (210x297 mm), Actual (148x210 mm A5)
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("finishedWidthMm" to "210.00", "finishedHeightMm" to "297.00"),
            artworkMetadataMap = mapOf("widthMm" to BigDecimal("148.00"), "heightMm" to BigDecimal("210.00"))
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("148.00x210.00 mm"))
    }

    @Test
    fun test03_specOrientationMatchRule_portraitVsLandscape() = runBlocking {
        val rule = SpecOrientationMatchRule()

        // Expected PORTRAIT, Actual LANDSCAPE
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredOrientation" to "PORTRAIT"),
            artworkMetadataMap = mapOf("widthMm" to BigDecimal("297.00"), "heightMm" to BigDecimal("210.00"))
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("LANDSCAPE"))
    }

    @Test
    fun test04_specFormatMatchRule_requiredPdfVsActualJpeg() = runBlocking {
        val rule = SpecFormatMatchRule()

        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredFormat" to "PDF"),
            artworkMetadataMap = mapOf("format" to "JPEG", "extension" to "jpg")
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("JPEG"))
    }

    @Test
    fun test05_specDocumentTypeMatchRule_catalogVsSinglePage() = runBlocking {
        val rule = SpecDocumentTypeMatchRule()

        // Catalog product requires multi-page document, but artwork has 1 page
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("productType" to "CATALOG"),
            artworkMetadataMap = mapOf("pageCount" to 1)
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("requires a multi-page document"))
    }

    @Test
    fun test06_partialSpecification_onlyExplicitAttributesEvaluated() = runBlocking {
        // Specification defines only requiredFormat = PDF
        val context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredFormat" to "PDF"),
            artworkMetadataMap = mapOf("format" to "PDF", "fileSize" to 1000L)
        )

        val result = service.runPreflight(context, "STAFF-01")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data
        assertEquals(PreflightOverallResult.PASS, run.overallResult)
    }

    @Test
    fun test07_fullPreflightRun_withSpecificationMismatch_returnsError() = runBlocking {
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            jobId = "JOB-SPEC-01",
            orderSpecificationMap = mapOf(
                "requiredPageCount" to 16,
                "finishedWidthMm" to "210.00",
                "finishedHeightMm" to "297.00",
                "requiredFormat" to "PDF"
            ),
            artworkMetadataMap = mapOf(
                "fileSize" to 5000000L,
                "format" to "PDF",
                "extension" to "pdf",
                "mimeType" to "application/pdf",
                "magicSignature" to "%PDF-1.7",
                "pageCount" to 12, // Page count mismatch (12 vs 16)
                "widthMm" to BigDecimal("210.00"),
                "heightMm" to BigDecimal("297.00")
            )
        )

        val result = service.runPreflight(mismatchContext, "STAFF-PREPRESS")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data

        assertEquals(PreflightRunStatus.COMPLETED, run.status)
        assertEquals(PreflightOverallResult.ERROR, run.overallResult)

        val findingsRes = service.listFindingsForRun(tenantId, run.preflightRunId)
        assertTrue(findingsRes is DomainResult.Success)
        val findings = (findingsRes as DomainResult.Success).data
        assertTrue(findings.any { it.ruleCode == "RULE_101_SPEC_PAGE_COUNT_MATCH" && it.severity == PreflightRuleSeverity.ERROR })
    }

    @Test
    fun test08_canonicalProductionWorkflow_regressionCheck() {
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

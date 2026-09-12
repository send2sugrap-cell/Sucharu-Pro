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

class AssetAndColorPreflightRulesTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP04"
    private val artworkId = "ARTWORK-ASSET-01"

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()

        // Register Step 01 - Step 04 Rules
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

        registry.registerRule(AssetDpiResolutionRule())
        registry.registerRule(AssetColorSpaceRule())
        registry.registerRule(AssetColorProfileRule())
        registry.registerRule(AssetIntegrityMissingRule())
        registry.registerRule(AssetIntegrityCorruptRule())

        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_dpiResolutionRule_withMinDpiReq_sufficientAndInsufficientDpi() = runBlocking {
        val rule = AssetDpiResolutionRule()

        // Sufficient DPI (300 DPI vs required 300 DPI) -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("minDpi" to 300),
            artworkMetadataMap = mapOf("resolutionDpi" to 300)
        )
        val passRes = rule.execute(passContext)
        assertEquals(PreflightExecutionResult.PASS, passRes.result)

        // Low DPI (150 DPI vs required 300 DPI) -> ERROR
        val failContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("minDpi" to 300),
            artworkMetadataMap = mapOf("resolutionDpi" to 150)
        )
        val failRes = rule.execute(failContext)
        assertEquals(PreflightExecutionResult.ERROR, failRes.result)
        assertTrue(failRes.findings.first().message.contains("below required minimum threshold"))
    }

    @Test
    fun test02_dpiResolutionRule_effectiveDpiCalculation_fromPixelsAndPhysicalSize() = runBlocking {
        val rule = AssetDpiResolutionRule()

        // widthPx = 2480, widthMm = 210 mm -> effective DPI = 2480 / (210 / 25.4) = ~300 DPI
        val calcContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("minDpi" to 300),
            artworkMetadataMap = mapOf("widthPx" to 2480, "widthMm" to 210.0)
        )
        val res = rule.execute(calcContext)
        assertEquals(PreflightExecutionResult.PASS, res.result)
    }

    @Test
    fun test03_dpiResolutionRule_missingRequirement_returnsInfo_notFalsePass() = runBlocking {
        val rule = AssetDpiResolutionRule()

        // No minDpi in specification, but artwork DPI extracted
        val noReqContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = emptyMap(),
            artworkMetadataMap = mapOf("resolutionDpi" to 200)
        )
        val res = rule.execute(noReqContext)
        assertEquals(PreflightExecutionResult.PASS, res.result)
        assertEquals(PreflightRuleSeverity.INFO, res.findings.first().severity)
        assertTrue(res.findings.first().message.contains("Extracted image resolution: 200 DPI"))
    }

    @Test
    fun test04_colorSpaceRule_matchingAndMismatchedColorSpace() = runBlocking {
        val rule = AssetColorSpaceRule()

        // Expected CMYK, Actual CMYK -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredColorSpace" to "CMYK"),
            artworkMetadataMap = mapOf("colorSpace" to "CMYK")
        )
        assertEquals(PreflightExecutionResult.PASS, rule.execute(passContext).result)

        // Expected CMYK, Actual RGB -> ERROR
        val failContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredColorSpace" to "CMYK"),
            artworkMetadataMap = mapOf("colorSpace" to "RGB")
        )
        val failRes = rule.execute(failContext)
        assertEquals(PreflightExecutionResult.ERROR, failRes.result)
        assertTrue(failRes.findings.first().message.contains("does not match required specification color space"))
    }

    @Test
    fun test05_colorSpaceRule_missingRequirement_returnsInfo_notFalsePass() = runBlocking {
        val rule = AssetColorSpaceRule()

        // No color space requirement in spec
        val noReqContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = emptyMap(),
            artworkMetadataMap = mapOf("colorSpace" to "RGB")
        )
        val res = rule.execute(noReqContext)
        assertEquals(PreflightExecutionResult.PASS, res.result)
        assertEquals(PreflightRuleSeverity.INFO, res.findings.first().severity)
        assertTrue(res.findings.first().message.contains("Detected artwork color space: RGB"))
    }

    @Test
    fun test06_colorProfileRule_matchingAndMismatchedIccProfile() = runBlocking {
        val rule = AssetColorProfileRule()

        // Required FOGRA39, Actual FOGRA39 -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredColorProfile" to "FOGRA39"),
            artworkMetadataMap = mapOf("colorProfile" to "ISO Coated v2 (ECI) / FOGRA39")
        )
        assertEquals(PreflightExecutionResult.PASS, rule.execute(passContext).result)

        // Required FOGRA39, Actual sRGB -> WARNING
        val warnContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requiredColorProfile" to "FOGRA39"),
            artworkMetadataMap = mapOf("colorProfile" to "sRGB IEC61966-2.1")
        )
        val warnRes = rule.execute(warnContext)
        assertEquals(PreflightExecutionResult.WARNING, warnRes.result)
        assertTrue(warnRes.findings.first().message.contains("does not match required profile"))
    }

    @Test
    fun test07_assetIntegrityMissingRule_missingAndIntactLinkedAssets() = runBlocking {
        val rule = AssetIntegrityMissingRule()

        // All linked assets present -> PASS
        val cleanContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("hasMissingAssets" to false, "missingAssets" to emptyList<String>())
        )
        assertTrue(rule.isApplicable(cleanContext))
        assertEquals(PreflightExecutionResult.PASS, rule.execute(cleanContext).result)

        // Missing linked asset -> ERROR
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("missingAssets" to listOf("logo_vector.eps", "background_highres.tif"))
        )
        val missingRes = rule.execute(missingContext)
        assertEquals(PreflightExecutionResult.ERROR, missingRes.result)
        assertTrue(missingRes.findings.first().message.contains("logo_vector.eps"))
    }

    @Test
    fun test08_assetIntegrityCorruptRule_corruptImageDetectedWithoutCrashingEngine() = runBlocking {
        val rule = AssetIntegrityCorruptRule()

        val corruptContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("corruptAssets" to listOf("product_hero.jpg"))
        )
        assertTrue(rule.isApplicable(corruptContext))

        val res = rule.execute(corruptContext)
        assertEquals(PreflightExecutionResult.ERROR, res.result)
        assertTrue(res.findings.first().message.contains("corrupted or unreadable: product_hero.jpg"))
    }

    @Test
    fun test09_fullPreflightRun_withStep01ToStep04Rules_returnsErrorForLowDpiAndRgb() = runBlocking {
        val context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            jobId = "JOB-STEP04-01",
            orderSpecificationMap = mapOf(
                "minDpi" to 300,
                "requiredColorSpace" to "CMYK"
            ),
            artworkMetadataMap = mapOf(
                "fileSize" to 5000000L,
                "format" to "PDF",
                "extension" to "pdf",
                "mimeType" to "application/pdf",
                "magicSignature" to "%PDF-1.7",
                "pageCount" to 1,
                "resolutionDpi" to 150, // Low DPI (150 vs 300) -> ERROR
                "colorSpace" to "RGB"    // RGB vs CMYK -> ERROR
            )
        )

        val result = service.runPreflight(context, "STAFF-PREPRESS")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data

        assertEquals(PreflightRunStatus.COMPLETED, run.status)
        assertEquals(PreflightOverallResult.ERROR, run.overallResult)

        val findingsRes = service.listFindingsForRun(tenantId, run.preflightRunId)
        assertTrue(findingsRes is DomainResult.Success)
        val findings = (findingsRes as DomainResult.Success).data
        assertTrue(findings.any { it.ruleCode == "RULE_201_ASSET_DPI_RESOLUTION" && it.severity == PreflightRuleSeverity.ERROR })
        assertTrue(findings.any { it.ruleCode == "RULE_202_ASSET_COLOR_SPACE" && it.severity == PreflightRuleSeverity.ERROR })
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

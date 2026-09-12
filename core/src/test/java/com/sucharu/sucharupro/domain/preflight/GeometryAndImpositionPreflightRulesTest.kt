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

class GeometryAndImpositionPreflightRulesTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP06"
    private val artworkId = "ARTWORK-GEOM-01"

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()

        // Register Step 01 - Step 06 Rules
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

        registry.registerRule(FontPresenceAndMissingRule())
        registry.registerRule(FontEmbeddingStatusRule())
        registry.registerRule(FontTypeCompatibilityRule())
        registry.registerRule(FontSubstitutionRule())
        registry.registerRule(FontRequirementComplianceRule())

        registry.registerRule(BleedBoxReadinessRule())
        registry.registerRule(TrimBoxPresenceAndSizeRule())
        registry.registerRule(PageBoxRelationshipRule())
        registry.registerRule(PageGeometryConsistencyRule())
        registry.registerRule(SafeAreaGeometryRule())
        registry.registerRule(ImpositionReadinessRule())

        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_bleedBoxReadinessRule_withAndWithoutMinBleedReq() = runBlocking {
        val rule = BleedBoxReadinessRule()

        // Bleed 3.0 mm vs minBleed 3.0 mm -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("minBleedMm" to 3.0),
            artworkMetadataMap = mapOf("bleedMarginMm" to 3.0)
        )
        val passRes = rule.execute(passContext)
        assertEquals(PreflightExecutionResult.PASS, passRes.result)

        // Bleed 1.0 mm vs minBleed 3.0 mm -> ERROR
        val errorContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("minBleedMm" to 3.0),
            artworkMetadataMap = mapOf("bleedMarginMm" to 1.0)
        )
        val errorRes = rule.execute(errorContext)
        assertEquals(PreflightExecutionResult.ERROR, errorRes.result)
        assertTrue(errorRes.findings.first().message.contains("less than required specification minimum"))
    }

    @Test
    fun test02_trimBoxPresenceAndSizeRule_matchingAndMismatchedTrimBox() = runBlocking {
        val rule = TrimBoxPresenceAndSizeRule()

        // Matching TrimBox (210x297 mm) -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("finishedWidthMm" to "210.00", "finishedHeightMm" to "297.00"),
            artworkMetadataMap = mapOf("hasTrimBox" to true, "trimBoxWidthMm" to 210.0, "trimBoxHeightMm" to 297.0)
        )
        val passRes = rule.execute(passContext)
        assertEquals(PreflightExecutionResult.PASS, passRes.result)

        // Mismatched TrimBox (148x210 mm A5 vs 210x297 mm A4 required) -> ERROR
        val errorContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("finishedWidthMm" to "210.00", "finishedHeightMm" to "297.00"),
            artworkMetadataMap = mapOf("hasTrimBox" to true, "trimBoxWidthMm" to 148.0, "trimBoxHeightMm" to 210.0)
        )
        val errorRes = rule.execute(errorContext)
        assertEquals(PreflightExecutionResult.ERROR, errorRes.result)
        assertTrue(errorRes.findings.first().message.contains("TrimBox dimensions"))
    }

    @Test
    fun test03_pageBoxRelationshipRule_validAndCorruptHierarchy() = runBlocking {
        val rule = PageBoxRelationshipRule()

        // Valid Hierarchy -> PASS
        val validContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("isPageBoxHierarchyValid" to true)
        )
        assertTrue(rule.isApplicable(validContext))
        assertEquals(PreflightExecutionResult.PASS, rule.execute(validContext).result)

        // TrimExceedsMedia -> ERROR
        val corruptContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("trimExceedsMedia" to true)
        )
        val corruptRes = rule.execute(corruptContext)
        assertEquals(PreflightExecutionResult.ERROR, corruptRes.result)
        assertTrue(corruptRes.findings.first().message.contains("TrimBox exceeds MediaBox boundary"))
    }

    @Test
    fun test04_pageGeometryConsistencyRule_uniformAndInconsistentPageGeometry() = runBlocking {
        val rule = PageGeometryConsistencyRule()

        // Multi-page document (16 pages) with uniform geometry -> PASS
        val uniformContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("pageCount" to 16, "isPageGeometryConsistent" to true)
        )
        assertTrue(rule.isApplicable(uniformContext))
        assertEquals(PreflightExecutionResult.PASS, rule.execute(uniformContext).result)

        // Multi-page document with mixed page sizes -> WARNING
        val mixedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("pageCount" to 16, "hasMixedPageSizes" to true)
        )
        val mixedRes = rule.execute(mixedContext)
        assertEquals(PreflightExecutionResult.WARNING, mixedRes.result)
        assertTrue(mixedRes.findings.first().message.contains("mixed trim sizes or orientations"))
    }

    @Test
    fun test05_impositionReadinessRule_bookletMultipleOf4PageCount() = runBlocking {
        val rule = ImpositionReadinessRule()

        // Booklet with 16 pages -> PASS
        val passBooklet = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("bindingMethod" to "SADDLE_STITCH"),
            artworkMetadataMap = mapOf("pageCount" to 16)
        )
        val passRes = rule.execute(passBooklet)
        assertEquals(PreflightExecutionResult.PASS, passRes.result)

        // Booklet with 14 pages (not multiple of 4) -> ERROR
        val failBooklet = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("bindingMethod" to "SADDLE_STITCH"),
            artworkMetadataMap = mapOf("pageCount" to 14)
        )
        val failRes = rule.execute(failBooklet)
        assertEquals(PreflightExecutionResult.ERROR, failRes.result)
        assertTrue(failRes.findings.first().message.contains("not a multiple of 4"))
    }

    @Test
    fun test06_fullPreflightRun_withInsufficientBleedAndNonMultipleOf4PageCount_returnsError() = runBlocking {
        val context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            jobId = "JOB-GEOM-01",
            orderSpecificationMap = mapOf(
                "minBleedMm" to 3.0,
                "bindingMethod" to "SADDLE_STITCH"
            ),
            artworkMetadataMap = mapOf(
                "fileSize" to 5000000L,
                "format" to "PDF",
                "extension" to "pdf",
                "mimeType" to "application/pdf",
                "pageCount" to 14, // Not multiple of 4 -> ERROR
                "bleedMarginMm" to 1.0 // 1mm < 3mm -> ERROR
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
        assertTrue(findings.any { it.ruleCode == "RULE_401_BLEED_GEOMETRY_READINESS" && it.severity == PreflightRuleSeverity.ERROR })
        assertTrue(findings.any { it.ruleCode == "RULE_406_IMPOSITION_READINESS" && it.severity == PreflightRuleSeverity.ERROR })
    }

    @Test
    fun test07_canonicalProductionWorkflow_regressionCheck() {
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

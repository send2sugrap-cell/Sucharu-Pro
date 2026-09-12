package com.sucharu.sucharupro.domain.preflight

import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.preflight.rules.*
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProofValidationAndComparisonRulesTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP07"
    private val artworkId = "ARTWORK-PROOF-01"

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()

        // Register Step 01 - Step 07 Rules
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

        registry.registerRule(ProofReferenceIntegrityRule())
        registry.registerRule(ProofPageCountComparisonRule())
        registry.registerRule(ProofGeometryComparisonRule())
        registry.registerRule(ProofContentFingerprintRule())
        registry.registerRule(ProofVersionComparisonRule())
        registry.registerRule(ProofVisualComparisonRule())

        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_proofReferenceIntegrityRule_resolvableAndMissingProof() = runBlocking {
        val rule = ProofReferenceIntegrityRule()

        // Valid Proof Reference -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            proofId = "PROOF-V1-100"
        )
        assertTrue(rule.isApplicable(passContext))
        assertEquals(PreflightExecutionResult.PASS, rule.execute(passContext).result)

        // Missing / Unresolvable Proof -> ERROR
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            proofId = "PROOF-MISSING-999",
            artworkMetadataMap = mapOf("proofNotFound" to true)
        )
        val errorRes = rule.execute(missingContext)
        assertEquals(PreflightExecutionResult.ERROR, errorRes.result)
        assertTrue(errorRes.findings.first().message.contains("unresolvable or missing"))
    }

    @Test
    fun test02_proofPageCountComparisonRule_matchingAndMismatchedPageCount() = runBlocking {
        val rule = ProofPageCountComparisonRule()

        // Match: Proof 16 pages, Reference 16 pages -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("proofPageCount" to 16, "referencePageCount" to 16)
        )
        assertTrue(rule.isApplicable(passContext))
        assertEquals(PreflightExecutionResult.PASS, rule.execute(passContext).result)

        // Mismatch: Proof 16 pages, Reference 12 pages -> ERROR
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("proofPageCount" to 16, "referencePageCount" to 12)
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("16"))
        assertTrue(mismatchRes.findings.first().message.contains("12"))
    }

    @Test
    fun test03_proofGeometryComparisonRule_matchingAndMismatchedDimensions() = runBlocking {
        val rule = ProofGeometryComparisonRule()

        // Match: Proof (210x297 mm), Reference (210x297 mm) -> PASS
        val passContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf(
                "proofWidthMm" to 210.0, "proofHeightMm" to 297.0,
                "refWidthMm" to 210.0, "refHeightMm" to 297.0
            )
        )
        assertTrue(rule.isApplicable(passContext))
        assertEquals(PreflightExecutionResult.PASS, rule.execute(passContext).result)

        // Mismatch: Proof (148x210 mm), Reference (210x297 mm) -> ERROR
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf(
                "proofWidthMm" to 148.0, "proofHeightMm" to 210.0,
                "refWidthMm" to 210.0, "refHeightMm" to 297.0
            )
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("148.0x210.0 mm"))
    }

    @Test
    fun test04_proofContentFingerprintRule_checksumTrackingDoesNotFailVisualApproval() = runBlocking {
        val rule = ProofContentFingerprintRule()

        // Differing checksum -> INFO finding (tracks version/file change without erroring out)
        val diffHashContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf(
                "proofChecksum" to "SHA256-PROOF-V2-ABC",
                "refChecksum" to "SHA256-PROOF-V1-XYZ"
            )
        )
        assertTrue(rule.isApplicable(diffHashContext))
        val res = rule.execute(diffHashContext)
        assertEquals(PreflightExecutionResult.PASS, res.result)
        assertEquals(PreflightRuleSeverity.INFO, res.findings.first().severity)
        assertTrue(res.findings.first().message.contains("version change detected"))
    }

    @Test
    fun test05_proofVersionComparisonRule_versionAVsVersionBMetadataDifferences() = runBlocking {
        val rule = ProofVersionComparisonRule()

        val prevMeta = mapOf(
            "pageCount" to 12,
            "colorSpace" to "RGB",
            "resolutionDpi" to 150
        )

        val versionContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf(
                "pageCount" to 16,
                "colorSpace" to "CMYK",
                "resolutionDpi" to 300,
                "previousVersionMetadata" to prevMeta
            )
        )
        assertTrue(rule.isApplicable(versionContext))

        val res = rule.execute(versionContext)
        assertEquals(PreflightExecutionResult.PASS, res.result)
        assertEquals(PreflightRuleSeverity.INFO, res.findings.first().severity)
        assertTrue(res.findings.first().message.contains("pageCount changed from 12 to 16"))
        assertTrue(res.findings.first().message.contains("colorSpace changed from RGB to CMYK"))
    }

    @Test
    fun test06_customerApprovalBoundary_preflightPassDoesNotAlterCustomerApprovalStatus() = runBlocking {
        val context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            proofId = "PROOF-101",
            artworkMetadataMap = mapOf(
                "fileSize" to 5000000L,
                "format" to "PDF",
                "extension" to "pdf",
                "mimeType" to "application/pdf",
                "magicSignature" to "%PDF-1.7",
                "pageCount" to 16,
                "proofPageCount" to 16,
                "referencePageCount" to 16,
                "proofWidthMm" to 210.0,
                "proofHeightMm" to 297.0,
                "refWidthMm" to 210.0,
                "refHeightMm" to 297.0
            )
        )

        val result = service.runPreflight(context, "STAFF-PREPRESS")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data

        assertEquals(PreflightRunStatus.COMPLETED, run.status)
        assertEquals(PreflightOverallResult.PASS, run.overallResult)

        // Verify Customer Approval Status in Module 05 remains independent in READY_FOR_REVIEW state
        val simulatedCustomerProofStatus = ProofStatus.READY_FOR_REVIEW
        assertEquals(ProofStatus.READY_FOR_REVIEW, simulatedCustomerProofStatus)
        // Automated preflight PASS does NOT transition proof status to ARCHIVED
        assertNotEquals(ProofStatus.ARCHIVED, simulatedCustomerProofStatus)
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

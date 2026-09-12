package com.sucharu.sucharupro.domain.preflight

import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.preflight.rules.FileExistenceRule
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PreflightProductionReadinessTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP09"
    private val artworkId = "ARTWORK-READY-01"

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()

        registry.registerRule(FileExistenceRule())

        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_validPreflightWithNoErrors_returnsReady() = runBlocking {
        val validContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to false, "fileUrl" to "https://storage.sucharu.com/artworks/v1.pdf")
        )

        val runRes = service.runPreflight(validContext, "STAFF-01")
        assertTrue(runRes is DomainResult.Success)

        val evalRes = service.evaluateProductionReadiness(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            evaluatorId = "STAFF-01"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.READY, readiness.decision)
        assertEquals(0, readiness.blockingFindingCount)
        assertTrue(readiness.blockingReasons.isEmpty())
    }

    @Test
    fun test02_unresolvedErrorFinding_returnsBlockedWithReasons() = runBlocking {
        val failingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )

        service.runPreflight(failingContext, "STAFF-01")

        val evalRes = service.evaluateProductionReadiness(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            evaluatorId = "STAFF-01"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.BLOCKED, readiness.decision)
        assertEquals(1, readiness.blockingFindingCount)
        assertFalse(readiness.blockingReasons.isEmpty())
        assertTrue(readiness.blockingReasons.first().contains("RULE_001_FILE_EXISTS"))
    }

    @Test
    fun test03_correctionSubmitted_notRevalidated_returnsBlocked() = runBlocking {
        val failingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )

        val run = (service.runPreflight(failingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        // Submit correction
        service.submitCorrection(
            tenantId = tenantId,
            findingId = finding.findingId,
            correctionType = PreflightCorrectionType.FILE_REPLACEMENT,
            description = "Uploaded replacement PDF",
            artworkVersionId = "V2",
            actorId = "DESIGNER-01"
        )

        // Evaluate readiness before revalidation
        val evalRes = service.evaluateProductionReadiness(
            tenantId = tenantId,
            artworkId = artworkId,
            evaluatorId = "STAFF-01"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.BLOCKED, readiness.decision)
        assertTrue(readiness.blockingReasons.first().contains("requires technical revalidation"))
    }

    @Test
    fun test04_revalidatedClearedDefect_returnsReady() = runBlocking {
        val failingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )

        val run = (service.runPreflight(failingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        service.submitCorrection(tenantId, finding.findingId, PreflightCorrectionType.FILE_REPLACEMENT, "Uploaded V2", "V2", "DESIGNER-01")

        // Revalidate with fixed context
        val fixedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V2",
            artworkMetadataMap = mapOf("fileNotFound" to false, "fileUrl" to "https://storage.sucharu.com/artworks/v2.pdf")
        )
        service.revalidateFinding(tenantId, finding.findingId, fixedContext, "STAFF-01")

        // Evaluate readiness
        val evalRes = service.evaluateProductionReadiness(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V2",
            evaluatorId = "STAFF-01"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.READY, readiness.decision)
        assertEquals(0, readiness.blockingFindingCount)
    }

    @Test
    fun test05_waivedErrorFinding_returnsReady() = runBlocking {
        val failingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )

        val run = (service.runPreflight(failingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        // Waive finding
        service.waiveFinding(tenantId, finding.findingId, "Manager approved waiver for press trial", "MGR-01")

        val evalRes = service.evaluateProductionReadiness(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            evaluatorId = "STAFF-01"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.READY, readiness.decision)
        assertEquals(0, readiness.blockingFindingCount)
    }

    @Test
    fun test06_stalePreflightVersion_returnsBlocked() = runBlocking {
        val v1Context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to false)
        )

        service.runPreflight(v1Context, "STAFF-01")

        // Request readiness for V2 when only V1 was preflighted
        val evalRes = service.evaluateProductionReadiness(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V2",
            evaluatorId = "STAFF-01"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.BLOCKED, readiness.decision)
        assertTrue(readiness.blockingReasons.first().contains("stale"))
    }

    @Test
    fun test07_missingPreflightRun_returnsBlocked() = runBlocking {
        val evalRes = service.evaluateProductionReadiness(
            tenantId = tenantId,
            artworkId = "NON-EXISTENT-ARTWORK",
            evaluatorId = "STAFF-01"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.BLOCKED, readiness.decision)
        assertTrue(readiness.blockingReasons.first().contains("No technical preflight run found"))
    }

    @Test
    fun test08_tenantIsolation_crossTenantReadiness_returnsBlocked() = runBlocking {
        val contextA = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to false)
        )
        service.runPreflight(contextA, "STAFF-01")

        // Tenant B trying to evaluate Tenant A artwork
        val evalRes = service.evaluateProductionReadiness(
            tenantId = "TENANT-B",
            artworkId = artworkId,
            evaluatorId = "STAFF-B"
        )
        assertTrue(evalRes is DomainResult.Success)
        val readiness = (evalRes as DomainResult.Success).data

        assertEquals(ProductionReadinessDecision.BLOCKED, readiness.decision)
        assertTrue(readiness.blockingReasons.first().contains("No technical preflight run found"))
    }

    @Test
    fun test09_canonicalProductionWorkflow_regressionCheck() {
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

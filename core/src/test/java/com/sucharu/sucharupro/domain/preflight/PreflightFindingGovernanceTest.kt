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

class PreflightFindingGovernanceTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP08"
    private val artworkId = "ARTWORK-GOV-01"

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
    fun test01_errorFindingCreated_inOpenStatus() = runBlocking {
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )

        val runRes = service.runPreflight(missingContext, "STAFF-01")
        assertTrue(runRes is DomainResult.Success)
        val run = (runRes as DomainResult.Success).data

        val findingsRes = service.listFindingsForRun(tenantId, run.preflightRunId)
        assertTrue(findingsRes is DomainResult.Success)
        val findings = (findingsRes as DomainResult.Success).data
        assertEquals(1, findings.size)

        val finding = findings.first()
        assertEquals(PreflightFindingStatus.OPEN, finding.status)
        assertEquals(PreflightRuleSeverity.ERROR, finding.severity)
    }

    @Test
    fun test02_acknowledgeFinding_transitionsToAcknowledged() = runBlocking {
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val run = (service.runPreflight(missingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        val ackRes = service.acknowledgeFinding(tenantId, finding.findingId, "STAFF-PREPRESS")
        assertTrue(ackRes is DomainResult.Success)
        val ackFinding = (ackRes as DomainResult.Success).data

        assertEquals(PreflightFindingStatus.ACKNOWLEDGED, ackFinding.status)
        assertEquals("STAFF-PREPRESS", ackFinding.acknowledgedBy)
        assertNotNull(ackFinding.acknowledgedAt)
    }

    @Test
    fun test03_submitCorrection_doesNotAutoResolveFinding_transitionsToRevalidationRequired() = runBlocking {
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val run = (service.runPreflight(missingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        val corrRes = service.submitCorrection(
            tenantId = tenantId,
            findingId = finding.findingId,
            correctionType = PreflightCorrectionType.FILE_REPLACEMENT,
            description = "Uploaded revised PDF artwork file with valid layers",
            artworkVersionId = "ARTWORK-V2",
            actorId = "STAFF-DESIGNER"
        )
        assertTrue(corrRes is DomainResult.Success)
        val corr = (corrRes as DomainResult.Success).data
        assertEquals("STAFF-DESIGNER", corr.submittedBy)

        // Verify finding status is REVALIDATION_REQUIRED (NOT automatically RESOLVED!)
        val updatedFinding = (service.getFindingDetails(tenantId, finding.findingId) as DomainResult.Success).data
        assertNotNull(updatedFinding)
        assertEquals(PreflightFindingStatus.REVALIDATION_REQUIRED, updatedFinding?.status)
        assertNotEquals(PreflightFindingStatus.RESOLVED, updatedFinding?.status)
    }

    @Test
    fun test04_revalidateFinding_clearedDefect_transitionsToResolved() = runBlocking {
        // 1. Initial failing run
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val run = (service.runPreflight(missingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        // 2. Submit correction
        service.submitCorrection(tenantId, finding.findingId, PreflightCorrectionType.FILE_REPLACEMENT, "Fixed file upload", "V2", "DESIGNER")

        // 3. Revalidate with fixed context (fileNotFound = false)
        val fixedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to false, "fileUrl" to "https://storage.sucharu.com/artworks/v2.pdf")
        )

        val revalRes = service.revalidateFinding(tenantId, finding.findingId, fixedContext, "STAFF-PREPRESS")
        assertTrue(revalRes is DomainResult.Success)
        val resolvedFinding = (revalRes as DomainResult.Success).data

        assertEquals(PreflightFindingStatus.RESOLVED, resolvedFinding.status)
        assertEquals("STAFF-PREPRESS", resolvedFinding.resolvedBy)
        assertNotNull(resolvedFinding.resolvedAt)
        assertNotNull(resolvedFinding.revalidationRunId)
        assertNotEquals(run.preflightRunId, resolvedFinding.revalidationRunId)
    }

    @Test
    fun test05_revalidateFinding_remainingDefect_staysInCorrectionRequired() = runBlocking {
        // 1. Initial failing run
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val run = (service.runPreflight(missingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        // 2. Revalidate with STILL failing context
        val revalRes = service.revalidateFinding(tenantId, finding.findingId, missingContext, "STAFF-PREPRESS")
        assertTrue(revalRes is DomainResult.Success)
        val stillFailingFinding = (revalRes as DomainResult.Success).data

        assertEquals(PreflightFindingStatus.CORRECTION_REQUIRED, stillFailingFinding.status)
        assertNull(stillFailingFinding.resolvedAt)
    }

    @Test
    fun test06_waiveFinding_authorizedManager_transitionsToWaivedWithJustification() = runBlocking {
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val run = (service.runPreflight(missingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        val waiveRes = service.waiveFinding(tenantId, finding.findingId, "Approved override by prepress manager per customer waiver email", "MGR-01")
        assertTrue(waiveRes is DomainResult.Success)
        val waivedFinding = (waiveRes as DomainResult.Success).data

        assertEquals(PreflightFindingStatus.WAIVED, waivedFinding.status)
        assertEquals("Approved override by prepress manager per customer waiver email", waivedFinding.waiverReason)
        assertEquals("MGR-01", waivedFinding.waivedBy)
        assertNotNull(waivedFinding.waivedAt)
    }

    @Test
    fun test07_terminalStatus_cannotBeModified() = runBlocking {
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val run = (service.runPreflight(missingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        service.waiveFinding(tenantId, finding.findingId, "Manager Waiver", "MGR-01")

        // Attempting to acknowledge a WAIVED finding -> Fails
        val ackRes = service.acknowledgeFinding(tenantId, finding.findingId, "STAFF-01")
        assertTrue(ackRes is DomainResult.Error)
        val err = ackRes as DomainResult.Error
        assertTrue(err.message.contains("Terminal finding status 'WAIVED' cannot be modified"))
    }

    @Test
    fun test08_tenantIsolation_crossTenantFindingAccess_rejected() = runBlocking {
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val run = (service.runPreflight(missingContext, "STAFF-01") as DomainResult.Success).data
        val finding = (service.listFindingsForRun(tenantId, run.preflightRunId) as DomainResult.Success).data.first()

        // Tenant B attempting to waive Tenant A finding
        val waiveRes = service.waiveFinding("TENANT-B", finding.findingId, "Cross tenant waiver attempt", "MGR-B")
        assertTrue(waiveRes is DomainResult.Error)
        val err = waiveRes as DomainResult.Error
        assertTrue(err.message.contains("not found"))
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

package com.sucharu.sucharupro.backend.preflight

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.preflight.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngine
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.preflight.*
import com.sucharu.sucharupro.domain.preflight.rules.FileExistenceRule
import com.sucharu.sucharupro.domain.repository.preflight.PreflightRepository
import com.sucharu.sucharupro.domain.service.preflight.PreflightService
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PreflightApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val artworkId = "ARTWORK-E2E-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr_01",
        projectId = projectId,
        username = "mgr_user",
        role = UserRole.MANAGER
    )

    private val guestPrincipal = AuthenticatedPrincipal(
        userId = "guest_01",
        projectId = projectId,
        username = "guest_user",
        role = UserRole.GUEST
    )

    @Before
    fun setup() = runBlocking {
        val preflightDs = FakePreflightDataSource()
        val preflightRepo = PreflightRepositoryImpl(preflightDs)
        val ruleRegistry = PreflightRuleRegistry()
        ruleRegistry.registerRule(FileExistenceRule())

        val preflightEngine = PreflightEngineImpl(ruleRegistry, preflightRepo)
        val preflightService = PreflightServiceImpl(preflightEngine, preflightRepo)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createPreflightRuleRegistry(): PreflightRuleRegistry = ruleRegistry
            override fun createPreflightRepository(tenantId: String): PreflightRepository = preflightRepo
            override fun createPreflightEngine(tenantId: String): PreflightEngine = preflightEngine
            override fun createPreflightService(tenantId: String): PreflightService = preflightService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_startPreflightRun_staff_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            jobId = "JOB-API-01"
        )

        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        assertNotNull(runResp.preflightRunId)
        assertEquals(artworkId, runResp.artworkId)
        assertEquals(PreflightRunStatus.COMPLETED, runResp.status)

        // Get details
        val details = useCases.getPreflightRunDetails(staffPrincipal, runResp.preflightRunId, customFactory)
        assertNotNull(details)
        assertEquals(runResp.preflightRunId, details?.preflightRunId)
    }

    @Test
    fun test02_startPreflightRun_guestRole_forbidden() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId
        )

        try {
            useCases.startPreflightRun(guestPrincipal, req, customFactory)
            fail("Expected ForbiddenException for Guest starting preflight run.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test03_acknowledgeFinding_staff_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        val findings = useCases.listPreflightFindings(staffPrincipal, runResp.preflightRunId, customFactory)
        assertEquals(1, findings.size)
        val finding = findings.first()

        val ackResp = useCases.acknowledgeFinding(staffPrincipal, finding.findingId, customFactory)
        assertEquals(PreflightFindingStatus.ACKNOWLEDGED, ackResp.status)
        assertEquals("staff_01", ackResp.acknowledgedBy)
    }

    @Test
    fun test04_submitFindingCorrection_staff_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        val finding = useCases.listPreflightFindings(staffPrincipal, runResp.preflightRunId, customFactory).first()

        val corrReq = SubmitCorrectionRequestDto(
            correctionType = PreflightCorrectionType.FILE_REPLACEMENT,
            description = "Uploaded corrected PDF file v2",
            artworkVersionId = "ARTWORK-V2"
        )
        val corrResp = useCases.submitFindingCorrection(staffPrincipal, finding.findingId, corrReq, customFactory)
        assertNotNull(corrResp.correctionId)
        assertEquals("staff_01", corrResp.submittedBy)
        assertEquals("Uploaded corrected PDF file v2", corrResp.description)
    }

    @Test
    fun test05_revalidateFinding_staff_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        val finding = useCases.listPreflightFindings(staffPrincipal, runResp.preflightRunId, customFactory).first()

        val revalReq = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to false, "fileUrl" to "https://storage.sucharu.com/artworks/v2.pdf")
        )
        val revalResp = useCases.revalidateFinding(staffPrincipal, finding.findingId, revalReq, customFactory)
        assertEquals(PreflightFindingStatus.RESOLVED, revalResp.status)
        assertEquals("staff_01", revalResp.resolvedBy)
        assertNotNull(revalResp.revalidationRunId)
    }

    @Test
    fun test06_waiveFinding_manager_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        val finding = useCases.listPreflightFindings(staffPrincipal, runResp.preflightRunId, customFactory).first()

        val waiveReq = WaiveFindingRequestDto(
            waiverReason = "Approved override by prepress manager"
        )
        val waivedResp = useCases.waiveFinding(managerPrincipal, finding.findingId, waiveReq, customFactory)
        assertEquals(PreflightFindingStatus.WAIVED, waivedResp.status)
        assertEquals("Approved override by prepress manager", waivedResp.waiverReason)
        assertEquals("mgr_01", waivedResp.waivedBy)
    }

    @Test
    fun test07_waiveFinding_staffRole_forbidden() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        val finding = useCases.listPreflightFindings(staffPrincipal, runResp.preflightRunId, customFactory).first()

        val waiveReq = WaiveFindingRequestDto(
            waiverReason = "Unauthorized staff waiver attempt"
        )
        try {
            useCases.waiveFinding(staffPrincipal, finding.findingId, waiveReq, customFactory)
            fail("Expected ForbiddenException when STAFF attempts to waive finding")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test08_listCorrectionsForFinding_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        val finding = useCases.listPreflightFindings(staffPrincipal, runResp.preflightRunId, customFactory).first()

        val corrReq = SubmitCorrectionRequestDto(
            correctionType = PreflightCorrectionType.FILE_REPLACEMENT,
            description = "Correction 1",
            artworkVersionId = "V2"
        )
        useCases.submitFindingCorrection(staffPrincipal, finding.findingId, corrReq, customFactory)

        val corrections = useCases.listCorrectionsForFinding(staffPrincipal, finding.findingId, customFactory)
        assertEquals(1, corrections.size)
        assertEquals("Correction 1", corrections.first().description)
    }

    @Test
    fun test09_evaluateProductionReadiness_ready_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to false, "fileUrl" to "https://storage.sucharu.com/artworks/v1.pdf")
        )
        useCases.startPreflightRun(staffPrincipal, req, customFactory)

        val evalReq = EvaluateProductionReadinessRequestDto(
            artworkId = artworkId,
            artworkVersionId = "V1"
        )
        val readinessResp = useCases.evaluateProductionReadiness(staffPrincipal, evalReq, customFactory)
        assertEquals(ProductionReadinessDecision.READY, readinessResp.decision)
        assertEquals(0, readinessResp.blockingFindingCount)
        assertTrue(readinessResp.blockingReasons.isEmpty())
    }

    @Test
    fun test10_evaluateProductionReadiness_blocked_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            artworkVersionId = "V1",
            artworkMetadataMap = mapOf("fileNotFound" to true)
        )
        useCases.startPreflightRun(staffPrincipal, req, customFactory)

        val evalReq = EvaluateProductionReadinessRequestDto(
            artworkId = artworkId,
            artworkVersionId = "V1"
        )
        val readinessResp = useCases.evaluateProductionReadiness(staffPrincipal, evalReq, customFactory)
        assertEquals(ProductionReadinessDecision.BLOCKED, readinessResp.decision)
        assertEquals(1, readinessResp.blockingFindingCount)
        assertFalse(readinessResp.blockingReasons.isEmpty())
    }
}


package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalPerformanceComplianceDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalPerformanceComplianceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class VendorPortalPerformanceApiTest {

    private lateinit var useCases: BackendUseCases

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor-user-1",
        projectId = "PRJ-001",
        username = "vendor1",
        role = UserRole.VENDOR,
        vendorId = "VND-API-01"
    )

    @Before
    fun setup() = runBlocking {
        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)

        vendorRepo.createVendor(
            Vendor(
                vendorId = "VND-API-01",
                projectId = "PRJ-001",
                vendorName = "Apex Quality Ltd",
                vendorCode = "APX-01",
                status = VendorStatus.ACTIVE
            )
        )

        val perfDs = FakeVendorPerformanceDataSource()
        val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
        val canonicalService = VendorPerformanceServiceImpl(perfRepo, vendorRepo)

        val portalDs = FakeVendorPortalPerformanceComplianceDataSource()
        val portalRepo = VendorPortalPerformanceComplianceRepositoryImpl(portalDs)

        val portalService = VendorPortalPerformanceComplianceServiceImpl(
            portalRepository = portalRepo,
            canonicalPerformanceService = canonicalService,
            vendorRepository = vendorRepo
        )

        // Seed Scorecard
        perfRepo.createScorecard(
            VendorPerformanceScorecard(
                scorecardId = "SC-001",
                tenantId = "PRJ-001",
                projectId = "PRJ-001",
                vendorId = "VND-API-01",
                periodType = EvaluationPeriodType.MONTHLY,
                periodStart = Instant.now().minus(30, ChronoUnit.DAYS),
                periodEnd = Instant.now(),
                overallScore = 94.0,
                rating = PerformanceRating.EXCELLENT,
                riskLevel = ComplianceRiskLevel.LOW,
                dataCompleteness = 100.0,
                sampleSize = 25,
                status = ScorecardStatus.APPROVED,
                items = emptyList(),
                generatedAt = Instant.now(),
                generatedBy = "SYSTEM"
            )
        )

        // Seed Evaluation
        perfRepo.createEvaluation(
            VendorEvaluation(
                evaluationId = "EV-001",
                tenantId = "PRJ-001",
                projectId = "PRJ-001",
                vendorId = "VND-API-01",
                scorecardId = "SC-001",
                periodType = EvaluationPeriodType.MONTHLY,
                periodStart = Instant.now().minus(30, ChronoUnit.DAYS),
                periodEnd = Instant.now(),
                evaluatorId = "EVALUATOR-01",
                evaluatorName = "Chief Quality Officer",
                status = EvaluationStatus.SUBMITTED,
                decision = EvaluationDecision.APPROVED,
                evaluationScore = 94.0,
                rating = PerformanceRating.EXCELLENT,
                createdAt = Instant.now(),
                createdBy = "SYSTEM"
            )
        )

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        // Custom test factory overriding the service
        val factory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createVendorPortalPerformanceComplianceService(tenantId: String): com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceService {
                return portalService
            }
        }

        useCases = BackendUseCases(fakeTxManager, factory)
    }

    @Test
    fun testGetPerformanceOverviewApi() = runBlocking {
        val overview = useCases.getVendorPortalPerformanceOverview(vendorPrincipal)
        assertEquals("VND-API-01", overview.vendorId)
        assertEquals(94.0, overview.overallScore, 0.001)
        assertEquals("EXCELLENT", overview.rating)
    }

    @Test
    fun testScorecardsAndEvaluationsApi() = runBlocking {
        val scorecards = useCases.listVendorPortalScorecards(vendorPrincipal)
        assertEquals(1, scorecards.size)

        val evals = useCases.listVendorPortalEvaluations(vendorPrincipal)
        assertEquals(1, evals.size)

        val ack = useCases.acknowledgeVendorEvaluation(vendorPrincipal, "EV-001")
        assertNotNull(ack.acknowledgedAt)
    }

    @Test
    fun testEvidenceUploadApi() = runBlocking {
        val uploadReq = VendorPortalComplianceEvidenceUploadRequest(
            recordId = "REC-01",
            requirementId = "REQ-01",
            actionId = null,
            evidenceType = "CERTIFICATE",
            fileName = "iso14001.pdf",
            fileUrl = "https://files.com/iso14001.pdf",
            description = "Environmental safety cert"
        )
        val evidence = useCases.uploadVendorPortalComplianceEvidence(vendorPrincipal, uploadReq)
        assertEquals("iso14001.pdf", evidence.fileName)

        val evidenceList = useCases.listVendorPortalComplianceEvidence(vendorPrincipal, "REC-01", null)
        assertEquals(1, evidenceList.size)
    }

    @Test
    fun testWorkspaceConsolidatedApi() = runBlocking {
        val ws = useCases.getVendorPortalPerformanceComplianceWorkspace(vendorPrincipal)
        assertEquals(94.0, ws.overview.overallScore, 0.001)
        assertEquals(1, ws.recentScorecards.size)
    }
}

package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalPerformanceComplianceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalPerformanceComplianceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalComplianceEvidenceType
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class VendorPortalPerformanceServiceTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-001"

    private lateinit var portalService: VendorPortalPerformanceComplianceServiceImpl
    private lateinit var canonicalService: VendorPerformanceServiceImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var portalRepo: VendorPortalPerformanceComplianceRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            val vendorDs = FakeVendorDataSource()
            vendorRepo = VendorRepositoryImpl(vendorDs)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorName = "Apex Supplies Ltd",
                    vendorCode = "APX-01",
                    status = VendorStatus.ACTIVE
                )
            )

            val perfDs = FakeVendorPerformanceDataSource()
            val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
            canonicalService = VendorPerformanceServiceImpl(perfRepo, vendorRepo)

            val portalDs = FakeVendorPortalPerformanceComplianceDataSource()
            portalRepo = VendorPortalPerformanceComplianceRepositoryImpl(portalDs)

            portalService = VendorPortalPerformanceComplianceServiceImpl(
                portalRepository = portalRepo,
                canonicalPerformanceService = canonicalService,
                vendorRepository = vendorRepo
            )

            // Seed Canonical Scorecard
            val scorecard = VendorPerformanceScorecard(
                scorecardId = "SC-001",
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                periodType = EvaluationPeriodType.MONTHLY,
                periodStart = Instant.now().minus(30, ChronoUnit.DAYS),
                periodEnd = Instant.now(),
                overallScore = 92.5,
                rating = PerformanceRating.EXCELLENT,
                riskLevel = ComplianceRiskLevel.LOW,
                dataCompleteness = 100.0,
                sampleSize = 25,
                status = ScorecardStatus.APPROVED,
                items = listOf(
                    VendorPerformanceScorecardItem(
                        itemId = "ITEM-01",
                        scorecardId = "SC-001",
                        kpiId = "KPI-OTD",
                        kpiCode = "OTD_RATE",
                        kpiName = "On-Time Delivery",
                        kpiType = KpiType.DELIVERY,
                        targetValue = 95.0,
                        actualValue = 96.0,
                        normalizedScore = 100.0,
                        weightedScore = 40.0,
                        numerator = 96.0,
                        denominator = 100.0,
                        weight = 0.4,
                        unit = "%",
                        direction = KpiDirection.HIGHER_IS_BETTER,
                        sampleSize = 25,
                        confidenceState = MeasurementConfidenceState.SUFFICIENT_DATA
                    )
                ),
                generatedAt = Instant.now(),
                generatedBy = "SYSTEM"
            )
            perfRepo.createScorecard(scorecard)

            // Seed Canonical Evaluation
            val eval = VendorEvaluation(
                evaluationId = "EV-001",
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                scorecardId = "SC-001",
                periodType = EvaluationPeriodType.MONTHLY,
                periodStart = Instant.now().minus(30, ChronoUnit.DAYS),
                periodEnd = Instant.now(),
                evaluatorId = "EVALUATOR-01",
                evaluatorName = "Chief Quality Officer",
                status = EvaluationStatus.SUBMITTED,
                decision = EvaluationDecision.APPROVED,
                evaluationScore = 92.5,
                rating = PerformanceRating.EXCELLENT,
                evaluatorComments = "Great job on lead times",
                createdAt = Instant.now(),
                createdBy = "SYSTEM"
            )
            perfRepo.createEvaluation(eval)

            // Seed Canonical Corrective Action
            val action = VendorCorrectiveAction(
                actionId = "CA-001",
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                sourceType = "QUALITY_AUDIT",
                issueDescription = "Minor calibration variation on packaging unit",
                actionPlan = "Recalibrate monthly",
                assignedTo = "QC-USER-01",
                assignedToName = "QC Lead",
                priority = CorrectiveActionPriority.HIGH,
                dueDate = Instant.now().plus(14, ChronoUnit.DAYS),
                status = CorrectiveActionStatus.OPEN,
                createdAt = Instant.now(),
                createdBy = "SYSTEM"
            )
            perfRepo.createCorrectiveAction(action)
        }
    }

    @Test
    fun testGetPerformanceOverview() {
        runBlocking {
            val res = portalService.getPerformanceOverview(tenantId, projectId, vendorId)
            assertTrue(res is DomainResult.Success)
            val overview = (res as DomainResult.Success).data
            assertEquals("VND-001", overview.vendorId)
            assertEquals(92.5, overview.overallScore, 0.001)
            assertEquals(PerformanceRating.EXCELLENT, overview.rating)
            assertEquals(1, overview.totalScorecards)
            assertEquals(1, overview.openCorrectiveActions)
        }
    }

    @Test
    fun testAcknowledgeEvaluation() {
        runBlocking {
            val res = portalService.acknowledgeEvaluation(tenantId, projectId, vendorId, "EV-001", "USER-VND-01")
            assertTrue(res is DomainResult.Success)
            val ack = (res as DomainResult.Success).data
            assertEquals("EV-001", ack.evaluationId)
            assertNotNull(ack.acknowledgedAt)
        }
    }

    @Test
    fun testSubmitEvaluationResponse() {
        runBlocking {
            val res = portalService.submitEvaluationResponse(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                evaluationId = "EV-001",
                subject = "Thank you for the high score",
                remarks = "We will maintain this OTD rate",
                proposedRemediation = null,
                evidenceReferences = emptyList(),
                actorId = "USER-VND-01"
            )
            assertTrue(res is DomainResult.Success)
            val response = (res as DomainResult.Success).data
            assertEquals("Thank you for the high score", response.subject)

            val listRes = portalService.listEvaluationResponses(tenantId, projectId, vendorId, "EV-001")
            assertTrue(listRes is DomainResult.Success)
            assertEquals(1, (listRes as DomainResult.Success).data.size)
        }
    }

    @Test
    fun testUploadComplianceEvidence() {
        runBlocking {
            val res = portalService.uploadComplianceEvidence(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                recordId = "REC-01",
                requirementId = "REQ-01",
                actionId = null,
                evidenceType = VendorPortalComplianceEvidenceType.CERTIFICATE,
                fileName = "tax_cert.pdf",
                fileUrl = "https://files.com/tax_cert.pdf",
                checksum = null,
                fileSizeBytes = 2048L,
                mimeType = "application/pdf",
                description = "Updated tax clearance certificate",
                actorId = "USER-VND-01"
            )
            assertTrue(res is DomainResult.Success)
            val evidence = (res as DomainResult.Success).data
            assertEquals("tax_cert.pdf", evidence.fileName)

            val listRes = portalService.listComplianceEvidence(tenantId, projectId, vendorId, "REC-01", null)
            assertTrue(listRes is DomainResult.Success)
            assertEquals(1, (listRes as DomainResult.Success).data.size)
        }
    }

    @Test
    fun testSubmitCorrectiveActionResponseAndCompletion() {
        runBlocking {
            val res = portalService.submitCorrectiveActionResponse(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                actionId = "CA-001",
                remediationNotes = "Sensors calibrated and logged",
                rootCauseExplanation = "Sensor drift due to temperature",
                progressPercentage = 80.0,
                evidenceReferences = emptyList(),
                actorId = "USER-VND-01"
            )
            assertTrue(res is DomainResult.Success)

            val compRes = portalService.submitCorrectiveActionCompletionRequest(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                actionId = "CA-001",
                completionNotes = "Full test run passed 100% inspection",
                evidenceReferences = emptyList(),
                actorId = "USER-VND-01"
            )
            assertTrue(compRes is DomainResult.Success)
        }
    }

    @Test
    fun testGetWorkspace() {
        runBlocking {
            val res = portalService.getWorkspace(tenantId, projectId, vendorId)
            assertTrue(res is DomainResult.Success)
            val ws = (res as DomainResult.Success).data
            assertEquals(92.5, ws.overview.overallScore, 0.001)
            assertEquals(1, ws.recentScorecards.size)
            assertEquals(1, ws.pendingEvaluations.size)
        }
    }
}

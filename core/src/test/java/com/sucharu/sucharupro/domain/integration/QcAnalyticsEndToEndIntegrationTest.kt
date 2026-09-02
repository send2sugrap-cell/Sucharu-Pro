package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import com.sucharu.sucharupro.domain.model.qc.analytics.QcInsightType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinalQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionDefectRepository
import com.sucharu.sucharupro.domain.repository.ProductionQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionReQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionReworkRepository
import com.sucharu.sucharupro.domain.repository.QcAnalyticsRepository
import com.sucharu.sucharupro.domain.repository.QcCostTimeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsEndToEndIntegrationTest {

    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var finalQcDataSource: FakeFinalQcDataSource
    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource

    private lateinit var qcRepository: ProductionQcRepository
    private lateinit var defectRepository: ProductionDefectRepository
    private lateinit var reworkRepository: ProductionReworkRepository
    private lateinit var reQcRepository: ProductionReQcRepository
    private lateinit var finalQcRepository: FinalQcRepository
    private lateinit var costTimeRepository: QcCostTimeRepository
    private lateinit var analyticsRepository: QcAnalyticsRepository

    private val testProjectId = "PRJ-E2E-09"
    private val testJobId = "JOB-E2E-09"

    @Before
    fun setup() {
        jobDataSource = FakeProductionJobDataSource()
        qcDataSource = FakeProductionQcDataSource()
        checklistDataSource = FakeQcChecklistDataSource()
        defectDataSource = FakeProductionDefectDataSource()
        reworkDataSource = FakeProductionReworkDataSource()
        reQcDataSource = FakeProductionReQcDataSource()
        finalQcDataSource = FakeFinalQcDataSource()
        costTimeDataSource = FakeQcCostTimeDataSource()
        analyticsDataSource = FakeQcAnalyticsDataSource()

        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
        defectRepository = ProductionDefectRepositoryImpl(defectDataSource)
        reworkRepository = ProductionReworkRepositoryImpl(
            reworkDataSource = reworkDataSource,
            defectDataSource = defectDataSource,
            qcDataSource = qcDataSource,
            checklistDataSource = checklistDataSource
        )
        reQcRepository = ProductionReQcRepositoryImpl(
            reQcDataSource = reQcDataSource,
            reworkDataSource = reworkDataSource,
            defectDataSource = defectDataSource,
            qcDataSource = qcDataSource,
            checklistDataSource = checklistDataSource
        )
        finalQcRepository = FinalQcRepositoryImpl(
            finalQcDataSource = finalQcDataSource,
            productionJobDataSource = jobDataSource,
            qcDataSource = qcDataSource,
            checklistDataSource = checklistDataSource,
            defectDataSource = defectDataSource,
            reworkDataSource = reworkDataSource,
            reQcDataSource = reQcDataSource
        )
        costTimeRepository = QcCostTimeRepositoryImpl(
            costTimeDataSource = costTimeDataSource,
            productionJobDataSource = jobDataSource,
            qcDataSource = qcDataSource,
            checklistDataSource = checklistDataSource,
            defectDataSource = defectDataSource,
            reworkDataSource = reworkDataSource,
            reQcDataSource = reQcDataSource,
            finalQcDataSource = finalQcDataSource
        )
        analyticsRepository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            qcDataSource = qcDataSource,
            defectDataSource = defectDataSource,
            reworkDataSource = reworkDataSource,
            reQcDataSource = reQcDataSource,
            finalQcDataSource = finalQcDataSource,
            qcCostTimeDataSource = costTimeDataSource
        )
    }

    @Test
    fun `full end-to-end QC quality pipeline into analytics insights and metrics`() = runBlocking {
        // ==========================================
        // 1. Production Job Setup
        // ==========================================
        val job = ProductionJob(
            jobId = testJobId,
            jobNumber = "JOB-ANALYTICS-E2E",
            orderId = testProjectId,
            orderNumber = "ORD-ANALYTICS-E2E",
            customerId = "cust-01",
            handoffId = "ho-e2e-01",
            title = "Hardcover Deluxe Book 2000 Qty",
            quantity = 2000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val jRes = jobDataSource.insertJob(job)
        assertTrue(jRes is DomainResult.Success)

        // ==========================================
        // 2. Pre-Production QC Pass
        // ==========================================
        val preQcRes = qcRepository.createQc(
            productionJobId = testJobId,
            qcType = QcType.PRE_PRODUCTION,
            notes = "Pre-Production plate and paper stock check",
            timestamp = "2026-08-17T08:30:00Z"
        )
        val preQcId = (preQcRes as DomainResult.Success).data.qcId

        qcRepository.startInspection(preQcId, "insp-01", timestamp = "2026-08-17T08:35:00Z")
        val itemsRes = qcRepository.initializePreProductionItems(preQcId, callerRole = UserRole.QC_INSPECTOR)
        if (itemsRes is DomainResult.Success) {
            for (item in itemsRes.data) {
                qcRepository.updatePreProductionItem(
                    itemId = item.itemId,
                    status = PreProductionItemStatus.PASS,
                    checkedBy = "insp-01",
                    checkedByName = "Tariq Inspector",
                    timestamp = "2026-08-17T08:40:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }
        val submitRes = qcRepository.submitPreProductionQc(
            qcId = preQcId,
            decision = QcDecision.PASS,
            submittedBy = "insp-01",
            submittedByName = "Tariq Inspector",
            notes = "All plates verified.",
            timestamp = "2026-08-17T08:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue("PreQc submit failed: ${(submitRes as? DomainResult.Error)?.message}", submitRes is DomainResult.Success)

        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = preQcId,
            costType = QcCostType.INSPECTION,
            description = "Pre-production test sheet testing",
            quantity = 1.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T08:45:00Z"
        )
        costTimeRepository.createTimeEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = preQcId,
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T08:30:00Z",
            endedAt = "2026-08-17T08:45:00Z",
            durationMinutes = 15L,
            timestamp = "2026-08-17T08:45:00Z"
        )

        // ==========================================
        // 3. Defect & Investigation
        // ==========================================
        val inlineQcRes = qcRepository.createQc(
            productionJobId = testJobId,
            qcType = QcType.FINAL,
            notes = "In-line production inspection",
            timestamp = "2026-08-17T09:00:00Z"
        )
        val inlineQcId = (inlineQcRes as DomainResult.Success).data.qcId

        val defectRes = defectRepository.createDefect(
            productionJobId = testJobId,
            qcId = inlineQcId,
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Cyan Misregistration",
            description = "Cyan shifted 1.2mm",
            affectedQuantity = 100,
            affectedUnit = "sheets",
            detectedBy = "insp-01",
            detectedByName = "Tariq Inspector",
            timestamp = "2026-08-17T09:10:00Z"
        )
        val defectId = (defectRes as DomainResult.Success).data.defectId
        defectRepository.acknowledgeDefect(defectId, "insp-01", timestamp = "2026-08-17T09:15:00Z")
        defectRepository.investigateDefect(defectId, "insp-01", timestamp = "2026-08-17T09:20:00Z")

        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionDefectId = defectId,
            costType = QcCostType.DEFECT_INVESTIGATION,
            description = "Cyan misregistration root cause analysis",
            quantity = 1.0,
            unitCost = 150.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:30:00Z"
        )

        // ==========================================
        // 4. Rework 1 & Re-QC 1 FAIL
        // ==========================================
        val rew1Res = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = inlineQcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 50,
            description = "Realign cyan plate",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T09:35:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew1Id = (rew1Res as DomainResult.Success).data.reworkId

        reworkRepository.startReview(rew1Id, "mgr-01", timestamp = "2026-08-17T09:36:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.approveRework(rew1Id, "mgr-01", timestamp = "2026-08-17T09:38:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.assignRework(rew1Id, "tech-01", "Kamal", "mgr-01", "Rahim", "Fix clamp", "2026-08-17T09:40:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rew1Id, "tech-01", timestamp = "2026-08-17T09:45:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rew1Id, "Clamp adjusted", 50, "tech-01", timestamp = "2026-08-17T09:55:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.returnToQc(rew1Id, "tech-01", timestamp = "2026-08-17T09:56:00Z", callerRole = UserRole.QC_INSPECTOR)

        val reqc1Res = reQcRepository.createReQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rew1Id,
            originalQcId = inlineQcId,
            originalDefectId = defectId,
            affectedQuantity = 50,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reqc1Id = (reqc1Res as DomainResult.Success).data.reQcId
        reQcRepository.startInspection(reqc1Id, "insp-01", timestamp = "2026-08-17T10:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        reQcRepository.failReQc(
            reQcId = reqc1Id,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Slight 0.4mm variance still visible",
            affectedQuantity = 10,
            inspectorId = "insp-01",
            inspectorName = "Tariq",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // ==========================================
        // 5. Rework 2 & Re-QC 2 PASS
        // ==========================================
        val rew2Res = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = inlineQcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 40,
            description = "Fine tune registration screws",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew2Id = (rew2Res as DomainResult.Success).data.reworkId

        reworkRepository.startReview(rew2Id, "mgr-01", timestamp = "2026-08-17T10:31:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.approveRework(rew2Id, "mgr-01", timestamp = "2026-08-17T10:32:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.assignRework(rew2Id, "tech-01", "Kamal", "mgr-01", "Rahim", "Fix", "2026-08-17T10:33:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rew2Id, "tech-01", timestamp = "2026-08-17T10:35:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rew2Id, "Fine tuned", 40, "tech-01", timestamp = "2026-08-17T10:50:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.returnToQc(rew2Id, "tech-01", timestamp = "2026-08-17T10:51:00Z", callerRole = UserRole.QC_INSPECTOR)

        val reqc2Res = reQcRepository.createNextCycle(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rew2Id,
            previousReQcId = reqc1Id,
            originalQcId = inlineQcId,
            originalDefectId = defectId,
            affectedQuantity = 40,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:55:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reqc2Id = (reqc2Res as DomainResult.Success).data.reQcId
        reQcRepository.startInspection(reqc2Id, "insp-01", timestamp = "2026-08-17T11:00:00Z", callerRole = UserRole.QC_INSPECTOR)
        reQcRepository.passReQc(
            reQcId = reqc2Id,
            inspectorId = "insp-01",
            inspectorName = "Tariq",
            passNotes = "All 40 sheets pass color registration perfectly",
            timestamp = "2026-08-17T11:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        defectRepository.resolveDefect(defectId, "Resolved after 2 passes", "insp-01", timestamp = "2026-08-17T11:20:00Z")

        // ==========================================
        // 6. Final QC Pass & Release
        // ==========================================
        val finalQcRes = finalQcRepository.createFinalQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            totalQuantity = 2000,
            preProductionQcId = preQcId,
            sourceDefectIds = listOf(defectId),
            sourceReworkIds = listOf(rew1Id, rew2Id),
            sourceReQcIds = listOf(reqc1Id, reqc2Id),
            notes = "Final inspection for 2000 hardcover books",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:25:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (finalQcRes as DomainResult.Success).data.finalQcId

        finalQcRepository.startInspection(finalQcId, "insp-01", "Tariq Inspector", timestamp = "2026-08-17T11:30:00Z", callerRole = UserRole.QC_INSPECTOR)
        finalQcRepository.submitPass(
            finalQcId = finalQcId,
            acceptedQuantity = 2000,
            notes = "2000 hardcover copies inspected and passed",
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            timestamp = "2026-08-17T11:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val relAuthRes = finalQcRepository.authorizeProductionRelease(
            finalQcId = finalQcId,
            releaseNotes = "Released to shipping",
            authorizedBy = "admin-01",
            authorizedByName = "Admin User",
            timestamp = "2026-08-17T11:50:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue("Release failed: ${(relAuthRes as? DomainResult.Error)?.message}", relAuthRes is DomainResult.Success)

        // Record Rework/Re-QC/Final QC Costs & Time
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rew1Id,
            costType = QcCostType.REWORK_QC,
            description = "Rework review 1",
            quantity = 1.0,
            unitCost = 120.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z"
        )
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            reQcId = reqc2Id,
            costType = QcCostType.RE_QC,
            description = "Re-QC 2 inspection",
            quantity = 1.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T11:15:00Z"
        )
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            finalQcId = finalQcId,
            costType = QcCostType.FINAL_QC,
            description = "Final batch check",
            quantity = 1.0,
            unitCost = 180.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T11:45:00Z"
        )

        // ==========================================
        // 7. Step 08 Reconciliation
        // ==========================================
        costTimeRepository.calculateReconciliation(
            productionJobId = testJobId,
            plannedCost = 300.0,
            plannedMinutes = 40L,
            reconciledBy = "mgr-01",
            notes = "Reconciliation complete",
            timestamp = "2026-08-17T12:00:00Z",
            callerRole = UserRole.MANAGER
        )

        // ==========================================
        // 8. Step 09 Analytics Verifications
        // ==========================================
        val period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z")

        // Summary
        val summaryRes = analyticsRepository.getSummary(period, testProjectId, UserRole.ADMIN)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(1, summary.totalJobs)
        assertEquals(650.0, summary.totalQcCost, 0.001) // 100 + 150 + 120 + 100 + 180 = 650.0
        assertEquals(1, summary.totalDefects)
        assertEquals(2, summary.totalReworks)
        assertEquals(2, summary.totalReQcCycles)
        assertEquals(100.0, summary.reworkRate, 0.001)
        assertEquals(100.0, summary.reQcRate, 0.001)
        assertEquals(0.0, summary.firstPassQcRate, 0.001) // Had failures -> 0%
        assertEquals(100.0, summary.finalQcPassRate, 0.001)
        assertEquals(1, summary.releasedJobCount)

        // Job Analytics
        val jobAnalyticsRes = analyticsRepository.getJobAnalytics(period, testProjectId, UserRole.ADMIN)
        assertTrue(jobAnalyticsRes is DomainResult.Success)
        val jobList = (jobAnalyticsRes as DomainResult.Success).data
        val jobStats = jobList.first()
        assertEquals(testJobId, jobStats.productionJobId)
        assertEquals(1, jobStats.defectCount)
        assertEquals(2, jobStats.reworkCount)
        assertEquals(2, jobStats.reQcCycleCount)
        assertTrue(jobStats.finalQcPassed)
        assertTrue(jobStats.productionReleased)
        assertFalse(jobStats.firstPassQc)
        assertTrue(jobStats.efficiencyScore < 100.0)

        // Defect Category Analytics
        val defectAnalyticsRes = analyticsRepository.getDefectAnalytics(period, testProjectId, UserRole.ADMIN)
        assertTrue(defectAnalyticsRes is DomainResult.Success)
        val defectList = (defectAnalyticsRes as DomainResult.Success).data
        val printQuality = defectList.find { it.defectCategory == DefectCategory.PRINT_QUALITY }
        assertTrue(printQuality != null)
        assertEquals(1, printQuality!!.defectCount)
        assertEquals(100, printQuality.affectedQuantity)

        // Production Stage Analytics
        val stageAnalyticsRes = analyticsRepository.getStageAnalytics(period, testProjectId, UserRole.ADMIN)
        assertTrue(stageAnalyticsRes is DomainResult.Success)
        val stageList = (stageAnalyticsRes as DomainResult.Success).data
        val printStage = stageList.find { it.productionStage == ProductionStageType.PRINTING }
        assertTrue(printStage != null)
        assertEquals(1, printStage!!.defectCount)

        // Trends
        val trendsRes = analyticsRepository.getTrends(period, testProjectId, UserRole.ADMIN)
        assertTrue(trendsRes is DomainResult.Success)
        val trends = (trendsRes as DomainResult.Success).data
        assertTrue(trends.isNotEmpty())

        // Operational Insights
        val insightsRes = analyticsRepository.getOperationalInsights(
            period = period,
            projectId = testProjectId,
            thresholdConfig = QcAnalyticsThresholdConfig(repeatedFailureCycleThreshold = 2),
            callerRole = UserRole.ADMIN
        )
        assertTrue(insightsRes is DomainResult.Success)
        val insights = (insightsRes as DomainResult.Success).data
        val repeatedFailure = insights.find { it.type == QcInsightType.REPEATED_FAILURE }
        assertTrue(repeatedFailure != null)
    }
}

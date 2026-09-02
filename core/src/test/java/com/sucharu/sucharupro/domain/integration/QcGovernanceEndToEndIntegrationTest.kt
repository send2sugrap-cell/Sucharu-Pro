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
import com.sucharu.sucharupro.data.datasource.FakeQcGovernanceDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.data.repository.QcGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
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
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcEscalationLevel
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionType
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementEffectiveness
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinalQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionDefectRepository
import com.sucharu.sucharupro.domain.repository.ProductionQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionReQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionReworkRepository
import com.sucharu.sucharupro.domain.repository.QcAnalyticsRepository
import com.sucharu.sucharupro.domain.repository.QcCostTimeRepository
import com.sucharu.sucharupro.domain.repository.QcGovernanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcGovernanceEndToEndIntegrationTest {

    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var finalQcDataSource: FakeFinalQcDataSource
    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var governanceDataSource: FakeQcGovernanceDataSource

    private lateinit var qcRepository: ProductionQcRepository
    private lateinit var defectRepository: ProductionDefectRepository
    private lateinit var reworkRepository: ProductionReworkRepository
    private lateinit var reQcRepository: ProductionReQcRepository
    private lateinit var finalQcRepository: FinalQcRepository
    private lateinit var costTimeRepository: QcCostTimeRepository
    private lateinit var analyticsRepository: QcAnalyticsRepository
    private lateinit var governanceRepository: QcGovernanceRepository

    private val testProjectId = "PRJ-GOV-E2E"
    private val testJobId = "JOB-GOV-E2E"

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
        governanceDataSource = FakeQcGovernanceDataSource()

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
        governanceRepository = QcGovernanceRepositoryImpl(
            governanceDataSource = governanceDataSource,
            analyticsRepository = analyticsRepository,
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
    fun `full end-to-end quality lifecycle into governance KPI evaluation, alert escalation, quality review and CQI CAPA action`() = runBlocking {
        // ==========================================
        // 1. Configure KPI Governance Targets
        // ==========================================
        governanceRepository.setTarget(
            QcKpiTarget(
                targetId = "TGT-FPR",
                projectId = testProjectId,
                kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
                targetValue = 95.0,
                minimumAcceptableValue = 85.0,
                effectiveFrom = "2026-08-01T00:00:00Z",
                configuredBy = "admin-01",
                createdAt = "2026-08-01T00:00:00Z",
                updatedAt = "2026-08-01T00:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        // ==========================================
        // 2. Production Job & Pre-Production QC Pass
        // ==========================================
        jobDataSource.insertJob(
            ProductionJob(
                jobId = testJobId,
                jobNumber = "JOB-GOV-001",
                orderId = testProjectId,
                orderNumber = "ORD-GOV-001",
                customerId = "cust-01",
                handoffId = "ho-gov-01",
                title = "Hardcover Edition 3000 Qty",
                quantity = 3000,
                status = ProductionJobStatus.IN_PROGRESS,
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )

        val preQcRes = qcRepository.createQc(
            productionJobId = testJobId,
            qcType = QcType.PRE_PRODUCTION,
            notes = "Paper and plate checks",
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
        qcRepository.submitPreProductionQc(
            qcId = preQcId,
            decision = QcDecision.PASS,
            submittedBy = "insp-01",
            submittedByName = "Tariq Inspector",
            notes = "Plates verified",
            timestamp = "2026-08-17T08:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // ==========================================
        // 3. In-Line Defect, Rework & Re-QC
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
            title = "Cyan Streak Defect",
            description = "Cyan roller line",
            affectedQuantity = 100,
            affectedUnit = "sheets",
            detectedBy = "insp-01",
            detectedByName = "Tariq Inspector",
            timestamp = "2026-08-17T09:10:00Z"
        )
        val defectId = (defectRes as DomainResult.Success).data.defectId
        defectRepository.acknowledgeDefect(defectId, "insp-01", timestamp = "2026-08-17T09:15:00Z")
        defectRepository.investigateDefect(defectId, "insp-01", timestamp = "2026-08-17T09:20:00Z")

        val rewRes = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = inlineQcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            description = "Clean ink rollers",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T09:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rewId = (rewRes as DomainResult.Success).data.reworkId
        reworkRepository.startReview(rewId, "mgr-01", timestamp = "2026-08-17T09:31:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.approveRework(rewId, "mgr-01", timestamp = "2026-08-17T09:32:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.assignRework(rewId, "tech-01", "Kamal", "mgr-01", "Rahim", "Fix", "2026-08-17T09:33:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rewId, "tech-01", timestamp = "2026-08-17T09:35:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rewId, "Cleaned", 100, "tech-01", timestamp = "2026-08-17T09:50:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.returnToQc(rewId, "tech-01", timestamp = "2026-08-17T09:51:00Z", callerRole = UserRole.QC_INSPECTOR)

        val reqcRes = reQcRepository.createReQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rewId,
            originalQcId = inlineQcId,
            originalDefectId = defectId,
            affectedQuantity = 100,
            createdBy = "insp-01",
            timestamp = "2026-08-17T09:55:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reqcId = (reqcRes as DomainResult.Success).data.reQcId
        reQcRepository.startInspection(reqcId, "insp-01", timestamp = "2026-08-17T10:00:00Z", callerRole = UserRole.QC_INSPECTOR)
        reQcRepository.passReQc(
            reQcId = reqcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq",
            passNotes = "Roller clean, print passed",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        defectRepository.resolveDefect(defectId, "Resolved", "insp-01", timestamp = "2026-08-17T10:20:00Z")

        // ==========================================
        // 4. Final QC & Production Release
        // ==========================================
        val finalQcRes = finalQcRepository.createFinalQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            totalQuantity = 3000,
            preProductionQcId = preQcId,
            sourceDefectIds = listOf(defectId),
            sourceReworkIds = listOf(rewId),
            sourceReQcIds = listOf(reqcId),
            notes = "Final inspection for 3000 books",
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:25:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (finalQcRes as DomainResult.Success).data.finalQcId
        finalQcRepository.startInspection(finalQcId, "insp-01", timestamp = "2026-08-17T10:30:00Z", callerRole = UserRole.QC_INSPECTOR)
        finalQcRepository.submitPass(
            finalQcId = finalQcId,
            acceptedQuantity = 3000,
            notes = "All copies passed",
            inspectorId = "insp-01",
            inspectorName = "Tariq",
            timestamp = "2026-08-17T10:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        finalQcRepository.authorizeProductionRelease(
            finalQcId = finalQcId,
            releaseNotes = "Authorized release to shipping",
            authorizedBy = "admin-01",
            authorizedByName = "Admin User",
            timestamp = "2026-08-17T10:50:00Z",
            callerRole = UserRole.ADMIN
        )

        // ==========================================
        // 5. Governance Evaluation & Alert Escalation
        // ==========================================
        val period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z")
        val evalRes = governanceRepository.evaluateProjectKpis(period, testProjectId, UserRole.ADMIN)
        assertTrue(evalRes is DomainResult.Success)
        val evals = (evalRes as DomainResult.Success).data

        // First pass rate was 0% (had rework/defect) vs 95% target -> CRITICAL_BREACH
        val fprEval = evals.find { it.kpiType == QcGovernanceKpi.FIRST_PASS_RATE }
        assertTrue(fprEval != null)
        assertEquals(QcThresholdStatus.CRITICAL_BREACH, fprEval!!.status)

        // Create Alert
        val alertRes = governanceRepository.createAlert(
            QcQualityAlert(
                alertId = "ALT-E2E-01",
                projectId = testProjectId,
                jobId = testJobId,
                kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
                currentValue = 0.0,
                targetValue = 95.0,
                severity = QcAlertSeverity.CRITICAL,
                title = "Zero First-Pass QC Rate on Job",
                message = "Job encountered in-line defect requiring roller clean",
                detectedAt = "2026-08-17T11:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )
        assertTrue(alertRes is DomainResult.Success)

        // Acknowledge Alert
        governanceRepository.acknowledgeAlert("ALT-E2E-01", "insp-01", "Investigated roller", "2026-08-17T11:05:00Z", UserRole.QC_INSPECTOR)

        // Escalate Alert
        val escRes = governanceRepository.escalateAlert(
            alertId = "ALT-E2E-01",
            targetLevel = QcEscalationLevel.MANAGER,
            escalatedBy = "insp-01",
            notes = "Escalating for preventive maintenance decision",
            timestamp = "2026-08-17T11:10:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(escRes is DomainResult.Success)

        // ==========================================
        // 6. Quality Review
        // ==========================================
        val reviewRes = governanceRepository.createReview(
            QcQualityReview(
                reviewId = "REV-E2E-01",
                projectId = testProjectId,
                title = "Monthly Quality Review - Hardcover Batch",
                reviewPeriod = period,
                openAlertIds = listOf("ALT-E2E-01"),
                reviewerId = "mgr-01",
                createdAt = "2026-08-17T11:15:00Z",
                updatedAt = "2026-08-17T11:15:00Z"
            ),
            callerRole = UserRole.MANAGER
        )
        assertTrue(reviewRes is DomainResult.Success)

        governanceRepository.startReview("REV-E2E-01", "mgr-01", "2026-08-17T11:20:00Z", UserRole.MANAGER)
        governanceRepository.completeReview(
            reviewId = "REV-E2E-01",
            reviewerId = "mgr-01",
            recommendations = "Initiate preventative ink roller maintenance procedure",
            reviewNotes = "Review concluded",
            timestamp = "2026-08-17T11:30:00Z",
            callerRole = UserRole.MANAGER
        )

        // ==========================================
        // 7. Improvement Action (CAPA) Lifecycle & Effectiveness
        // ==========================================
        val actionRes = governanceRepository.proposeImprovementAction(
            QcImprovementAction(
                actionId = "ACT-E2E-01",
                projectId = testProjectId,
                sourceAlertId = "ALT-E2E-01",
                sourceReviewId = "REV-E2E-01",
                relatedJobId = testJobId,
                proposedBy = "insp-01",
                proposedByName = "Tariq Inspector",
                actionType = QcImprovementActionType.PREVENTIVE_ACTION,
                title = "Daily Ink Roller Solvent Calibration",
                description = "Standardize daily solvent checks prior to run start",
                baselineKpiValue = 0.0,
                createdAt = "2026-08-17T11:35:00Z",
                updatedAt = "2026-08-17T11:35:00Z"
            ),
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(actionRes is DomainResult.Success)

        governanceRepository.approveImprovementAction("ACT-E2E-01", "mgr-01", "Manager Rahim", "2026-08-17T11:40:00Z", UserRole.MANAGER)
        governanceRepository.assignImprovementAction("ACT-E2E-01", "tech-01", "Kamal Technician", "2026-08-17T11:45:00Z", UserRole.MANAGER)
        governanceRepository.startImprovementAction("ACT-E2E-01", "2026-08-17T11:50:00Z", UserRole.QC_INSPECTOR)
        governanceRepository.completeImprovementAction("ACT-E2E-01", "Daily check procedure posted and trained", "2026-08-17T12:00:00Z", 98.0, UserRole.QC_INSPECTOR)

        val verifyRes = governanceRepository.verifyImprovementAction(
            actionId = "ACT-E2E-01",
            verifiedBy = "admin-01",
            verifiedByName = "Admin User",
            effectiveness = QcImprovementEffectiveness.HIGHLY_EFFECTIVE,
            verificationNotes = "First-pass rate achieved 98% in subsequent runs",
            timestamp = "2026-08-17T12:15:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(verifyRes is DomainResult.Success)

        // Resolve Alert
        governanceRepository.resolveAlert("ALT-E2E-01", "mgr-01", "CAPA action verified effective", "2026-08-17T12:20:00Z", UserRole.MANAGER)

        // ==========================================
        // 8. Governance Snapshot & Audit Verification
        // ==========================================
        val snapshotRes = governanceRepository.createSnapshot(period, testProjectId, "admin-01", "2026-08-17T12:30:00Z", UserRole.ADMIN)
        assertTrue(snapshotRes is DomainResult.Success)
        val snapshot = (snapshotRes as DomainResult.Success).data
        assertEquals(testProjectId, snapshot.projectId)
        assertEquals(1, snapshot.totalAlertCount)
        assertEquals(0, snapshot.openCriticalAlertCount) // Alert was resolved!

        val auditEvents = governanceRepository.observeActivityEvents(testProjectId).first()
        assertTrue(auditEvents.size >= 8)
    }
}

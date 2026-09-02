package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityType
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinalQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionDefectRepository
import com.sucharu.sucharupro.domain.repository.ProductionQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionReQcRepository
import com.sucharu.sucharupro.domain.repository.ProductionReworkRepository
import com.sucharu.sucharupro.domain.repository.QcCostTimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeEndToEndIntegrationTest {

    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var finalQcDataSource: FakeFinalQcDataSource

    private lateinit var qcRepository: ProductionQcRepository
    private lateinit var defectRepository: ProductionDefectRepository
    private lateinit var reworkRepository: ProductionReworkRepository
    private lateinit var reQcRepository: ProductionReQcRepository
    private lateinit var finalQcRepository: FinalQcRepository
    private lateinit var costTimeRepository: QcCostTimeRepository

    private val testJobId = "JOB-MASTER-E2E"
    private val testProjectId = "PRJ-MASTER-E2E"

    @Before
    fun setup() {
        costTimeDataSource = FakeQcCostTimeDataSource()
        jobDataSource = FakeProductionJobDataSource()
        qcDataSource = FakeProductionQcDataSource()
        checklistDataSource = FakeQcChecklistDataSource()
        defectDataSource = FakeProductionDefectDataSource()
        reworkDataSource = FakeProductionReworkDataSource()
        reQcDataSource = FakeProductionReQcDataSource()
        finalQcDataSource = FakeFinalQcDataSource()

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
    }

    @Test
    fun `full end-to-end QC cost and time lifecycle from production to locked reconciliation`() = runBlocking {
        // ==========================================
        // 1. Production Job Setup
        // ==========================================
        val job = ProductionJob(
            jobId = testJobId,
            jobNumber = "JOB-E2E",
            orderId = testProjectId,
            orderNumber = "ORD-E2E",
            customerId = "cust-01",
            handoffId = "ho-01",
            title = "High-End Catalog 1000 Qty",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        jobDataSource.insertJob(job)

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
        qcRepository.submitPreProductionQc(
            qcId = preQcId,
            decision = QcDecision.PASS,
            submittedBy = "insp-01",
            submittedByName = "Tariq Inspector",
            notes = "All 4-color plates and 150gsm paper stock verified.",
            timestamp = "2026-08-17T08:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // Record Cost & Time for Pre-Production QC
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = preQcId,
            costType = QcCostType.INSPECTION,
            description = "Pre-production proofing sheet testing",
            quantity = 1.0,
            unitCost = 150.0,
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
            title = "Cyan Streaking",
            description = "Cyan streaks on page 4",
            affectedQuantity = 100,
            affectedUnit = "sheets",
            detectedBy = "insp-01",
            detectedByName = "Tariq Inspector",
            timestamp = "2026-08-17T09:00:00Z"
        )
        val defectId = (defectRes as DomainResult.Success).data.defectId
        defectRepository.acknowledgeDefect(defectId, "insp-01", timestamp = "2026-08-17T09:05:00Z")
        defectRepository.investigateDefect(defectId, "insp-01", timestamp = "2026-08-17T09:10:00Z")

        // Record Cost & Time for Defect Investigation
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionDefectId = defectId,
            costType = QcCostType.DEFECT_INVESTIGATION,
            description = "Cyan roller inspection diagnostics",
            quantity = 1.0,
            unitCost = 200.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:30:00Z"
        )
        costTimeRepository.createTimeEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionDefectId = defectId,
            entryType = QcTimeEntryType.INVESTIGATION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:10:00Z",
            endedAt = "2026-08-17T09:30:00Z",
            durationMinutes = 20L,
            timestamp = "2026-08-17T09:30:00Z"
        )

        // ==========================================
        // 4. Rework Execution
        // ==========================================
        val rewRes = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = inlineQcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            description = "Reprint 100 cyan signature sheets",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T09:35:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rewId = (rewRes as DomainResult.Success).data.reworkId

        reworkRepository.startReview(rewId, "mgr-01", timestamp = "2026-08-17T09:36:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.approveRework(rewId, "mgr-01", timestamp = "2026-08-17T09:37:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.assignRework(rewId, "tech-01", "Kamal", "mgr-01", "Rahim", "Fix roller", "2026-08-17T09:38:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rewId, "tech-01", timestamp = "2026-08-17T09:40:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rewId, "Reprinted 100 sheets", 100, "tech-01", timestamp = "2026-08-17T10:15:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.returnToQc(rewId, "tech-01", timestamp = "2026-08-17T10:16:00Z", callerRole = UserRole.QC_INSPECTOR)

        // Record Cost & Time for Rework Quality Review
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rewId,
            costType = QcCostType.REWORK_QC,
            description = "Rework reprint inspection overhead",
            quantity = 1.0,
            unitCost = 180.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T10:20:00Z"
        )
        costTimeRepository.createTimeEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rewId,
            entryType = QcTimeEntryType.REWORK_REVIEW,
            actorId = "insp-01",
            startedAt = "2026-08-17T10:15:00Z",
            endedAt = "2026-08-17T10:25:00Z",
            durationMinutes = 10L,
            timestamp = "2026-08-17T10:25:00Z"
        )

        // ==========================================
        // 5. Re-QC Inspection Pass
        // ==========================================
        val reQcRes = reQcRepository.createReQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rewId,
            originalQcId = inlineQcId,
            originalDefectId = defectId,
            affectedQuantity = 100,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (reQcRes as DomainResult.Success).data.reQcId

        reQcRepository.startInspection(reQcId, "insp-01", timestamp = "2026-08-17T10:35:00Z", callerRole = UserRole.QC_INSPECTOR)
        reQcRepository.passReQc(
            reQcId = reQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq",
            passNotes = "Reprint passed all color density checks",
            timestamp = "2026-08-17T10:50:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // Mark defect as resolved
        defectRepository.resolveDefect(defectId, "Reprint verified and passed Re-QC", "insp-01", timestamp = "2026-08-17T10:55:00Z")

        // Record Cost & Time for Re-QC
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            reQcId = reQcId,
            costType = QcCostType.RE_QC,
            description = "Re-QC spectro test sheets",
            quantity = 1.0,
            unitCost = 120.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T10:55:00Z"
        )
        costTimeRepository.createTimeEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            reQcId = reQcId,
            entryType = QcTimeEntryType.RE_QC,
            actorId = "insp-01",
            startedAt = "2026-08-17T10:35:00Z",
            endedAt = "2026-08-17T10:50:00Z",
            durationMinutes = 15L,
            timestamp = "2026-08-17T10:50:00Z"
        )

        // ==========================================
        // 6. Final QC Pass & Release
        // ==========================================
        val finalQcRes = finalQcRepository.createFinalQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            totalQuantity = 1000,
            preProductionQcId = preQcId,
            sourceDefectIds = listOf(defectId),
            sourceReworkIds = listOf(rewId),
            sourceReQcIds = listOf(reQcId),
            notes = "Final comprehensive quality inspection for 1000 catalogs.",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (finalQcRes as DomainResult.Success).data.finalQcId

        finalQcRepository.startInspection(finalQcId, "insp-01", "Tariq Inspector", timestamp = "2026-08-17T11:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        finalQcRepository.submitPass(
            finalQcId = finalQcId,
            acceptedQuantity = 1000,
            notes = "All 1000 finished catalogs inspected and passed",
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            timestamp = "2026-08-17T11:35:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // Authorize production release
        finalQcRepository.authorizeProductionRelease(
            finalQcId = finalQcId,
            releaseNotes = "Final release authorized to shipping",
            authorizedBy = "admin-01",
            authorizedByName = "Admin User",
            timestamp = "2026-08-17T11:40:00Z",
            callerRole = UserRole.ADMIN
        )

        // Record Cost & Time for Final QC
        costTimeRepository.createCostEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            finalQcId = finalQcId,
            costType = QcCostType.FINAL_QC,
            description = "Final batch sample packaging inspection",
            quantity = 1.0,
            unitCost = 150.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T11:40:00Z"
        )
        costTimeRepository.createTimeEntry(
            projectId = testProjectId,
            productionJobId = testJobId,
            finalQcId = finalQcId,
            entryType = QcTimeEntryType.FINAL_QC,
            actorId = "insp-01",
            startedAt = "2026-08-17T11:05:00Z",
            endedAt = "2026-08-17T11:35:00Z",
            durationMinutes = 30L,
            timestamp = "2026-08-17T11:35:00Z"
        )

        // ==========================================
        // 7. Calculate Reconciliation
        // Total Actual Cost: 150 + 200 + 180 + 120 + 150 = 800.0 BDT
        // Total Actual Time: 15 + 20 + 10 + 15 + 30 = 90 mins
        // Planned: Cost 500.0 BDT, Time 45 mins
        // ==========================================
        val reconRes = costTimeRepository.calculateReconciliation(
            productionJobId = testJobId,
            plannedCost = 500.0,
            plannedMinutes = 45L,
            reconciledBy = "mgr-01",
            notes = "Post-release reconciliation",
            timestamp = "2026-08-17T12:00:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data
        assertEquals(800.0, recon.actualCost, 0.001)
        assertEquals(300.0, recon.costVariance, 0.001) // 800 - 500 = +300
        assertEquals(90L, recon.actualMinutes)
        assertEquals(45L, recon.timeVarianceMinutes) // 90 - 45 = +45
        assertEquals(5, recon.qcEntryCount)
        assertEquals(5, recon.timeEntryCount)
        assertEquals(1, recon.defectCount)
        assertEquals(1, recon.reworkCount)
        assertEquals(1, recon.reQcCycleCount)
        assertEquals(1, recon.finalQcCount)
        assertEquals(QcCostStatus.RECONCILED, recon.status)

        // ==========================================
        // 8. Adjust Reconciliation Benchmark
        // ==========================================
        val adjustRes = costTimeRepository.adjustReconciliation(
            reconciliationId = recon.id,
            adjustedPlannedCost = 600.0,
            adjustedPlannedMinutes = 60L,
            adjustmentReason = "Client requested rush reprint allowance",
            adjustedBy = "mgr-01",
            timestamp = "2026-08-17T12:10:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(adjustRes is DomainResult.Success)
        val adjustedRecon = (adjustRes as DomainResult.Success).data
        assertEquals(600.0, adjustedRecon.plannedCost, 0.001)
        assertEquals(200.0, adjustedRecon.costVariance, 0.001) // 800 - 600 = +200
        assertEquals(30L, adjustedRecon.timeVarianceMinutes) // 90 - 60 = +30
        assertEquals(QcCostStatus.ADJUSTED, adjustedRecon.status)

        // ==========================================
        // 9. Permanently Lock Reconciliation
        // ==========================================
        // Separation of duties test: QC Inspector is rejected
        val lockInspectorRes = costTimeRepository.lockReconciliation(
            reconciliationId = adjustedRecon.id,
            lockedBy = "insp-01",
            timestamp = "2026-08-17T12:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(lockInspectorRes is DomainResult.Error)

        // Manager / Admin locks successfully
        val lockAdminRes = costTimeRepository.lockReconciliation(
            reconciliationId = adjustedRecon.id,
            lockedBy = "admin-01",
            lockNotes = "Final audit complete. Sealing record.",
            timestamp = "2026-08-17T12:15:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(lockAdminRes is DomainResult.Success)
        val snapshot = (lockAdminRes as DomainResult.Success).data

        // ==========================================
        // 10. Verify Snapshot & Immutability
        // ==========================================
        assertEquals(testJobId, snapshot.productionJobId)
        assertEquals(testProjectId, snapshot.projectId)
        assertEquals(800.0, snapshot.actualCost, 0.001)
        assertEquals(200.0, snapshot.costVariance, 0.001)
        assertEquals(90L, snapshot.actualMinutes)
        assertEquals(30L, snapshot.timeVarianceMinutes)
        assertEquals(5, snapshot.costEntryIds.size)
        assertEquals(5, snapshot.timeEntryIds.size)
        assertEquals(listOf(defectId), snapshot.defectIds)
        assertEquals(listOf(rewId), snapshot.reworkIds)
        assertEquals(listOf(reQcId), snapshot.reQcIds)
        assertEquals(finalQcId, snapshot.finalQcId)

        // Verify all cost entries are LOCKED
        val costEntries = (costTimeRepository.getCostEntriesForJob(testJobId) as DomainResult.Success).data
        assertTrue(costEntries.all { it.isLocked && it.status == QcCostStatus.LOCKED })

        // Verify all time entries are LOCKED
        val timeEntries = (costTimeRepository.getTimeEntriesForJob(testJobId) as DomainResult.Success).data
        assertTrue(timeEntries.all { it.isLocked && it.status == QcTimeStatus.LOCKED })

        // Verify audit trail contains all events
        val auditEvents = costTimeRepository.observeActivityEvents(testJobId).first()
        val eventTypes = auditEvents.map { it.activityType }
        assertTrue(eventTypes.contains(QcCostTimeActivityType.QC_COST_ENTRY_CREATED))
        assertTrue(eventTypes.contains(QcCostTimeActivityType.QC_TIME_ENTRY_CREATED))
        assertTrue(eventTypes.contains(QcCostTimeActivityType.QC_RECONCILIATION_COMPLETED))
        assertTrue(eventTypes.contains(QcCostTimeActivityType.QC_RECONCILIATION_ADJUSTED))
        assertTrue(eventTypes.contains(QcCostTimeActivityType.QC_COST_TIME_SNAPSHOT_CREATED))
        assertTrue(eventTypes.contains(QcCostTimeActivityType.QC_RECONCILIATION_LOCKED))
    }
}

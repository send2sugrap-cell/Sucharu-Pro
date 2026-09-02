package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityType
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive End-to-End integration test covering the entire failure loop workflow:
 *
 * ProductionJob
 *     ↓
 * QC
 *     ↓
 * Defect
 *     ↓
 * Rework 1
 *     ↓
 * RETURNED_TO_QC
 *     ↓
 * Re-QC Cycle 1
 *     ↓
 * FAIL
 *     ↓
 * Failure Record
 *     ↓
 * Rework Cycle 2
 *     ↓
 * RETURNED_TO_QC
 *     ↓
 * Re-QC Cycle 2
 *     ↓
 * PASS
 *
 * (Module 06 Step 06 Integration Suite).
 */
class ReQcEndToEndIntegrationTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource

    private lateinit var qcRepository: ProductionQcRepositoryImpl
    private lateinit var defectRepository: ProductionDefectRepositoryImpl
    private lateinit var reworkRepository: ProductionReworkRepositoryImpl
    private lateinit var reQcRepository: ProductionReQcRepositoryImpl

    private val testProjectId = "proj-order-101"
    private val testJobId = "job-card-501"

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource()
        defectDataSource = FakeProductionDefectDataSource()
        reworkDataSource = FakeProductionReworkDataSource()
        reQcDataSource = FakeProductionReQcDataSource()
        checklistDataSource = FakeQcChecklistDataSource()

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
    }

    @Test
    fun fullEndToEndFailureLoopWorkflow_completesWithIntegrity() = runBlocking {
        // ==========================================
        // 1. Initial Production QC
        // ==========================================
        val qcRes = qcRepository.createQc(
            productionJobId = testJobId,
            qcType = QcType.PRE_PRODUCTION,
            notes = "QC inspection for book printing order",
            timestamp = "2026-08-17T08:00:00Z"
        )
        assertTrue(qcRes is DomainResult.Success)
        val qc = (qcRes as DomainResult.Success).data
        val qcId = qc.qcId

        // ==========================================
        // 2. Defect Detection during QC
        // ==========================================
        val defectRes = defectRepository.createDefect(
            productionJobId = testJobId,
            qcId = qcId,
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Severe Cyan Misalignment",
            description = "Color density on Cyan plate offset by 3.2 delta-E across 200 sheets",
            affectedQuantity = 200,
            affectedUnit = "sheets",
            detectedBy = "insp-tariq",
            detectedByName = "Tariq Inspector",
            timestamp = "2026-08-17T08:15:00Z"
        )
        assertTrue(defectRes is DomainResult.Success)
        val defect = (defectRes as DomainResult.Success).data
        val defectId = defect.defectId

        // ==========================================
        // 3. Rework Cycle 1 (Requested -> Approved -> Assigned -> In Progress -> Completed -> Returned to QC)
        // ==========================================
        val rework1Res = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = qcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 200,
            quantityUnit = "sheets",
            description = "Cyan plate realign and recalibrate ink flow",
            requestedBy = "insp-tariq",
            requestedByName = "Tariq Inspector",
            timestamp = "2026-08-17T08:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(rework1Res is DomainResult.Success)
        val rework1 = (rework1Res as DomainResult.Success).data
        val rework1Id = rework1.reworkId

        // Management review & approval
        reworkRepository.startReview(rework1Id, "mgr-rahim", "Rahim Manager", "Reviewing feasibility", "2026-08-17T08:35:00Z", UserRole.MANAGER)
        reworkRepository.approveRework(rework1Id, "mgr-rahim", "Rahim Manager", "Approved for plate technician", "2026-08-17T08:40:00Z", UserRole.MANAGER)

        // Operator assignment & execution
        reworkRepository.assignRework(rework1Id, "tech-kamal", "Kamal Technician", "mgr-rahim", "Rahim Manager", "Urgent plate fix", "2026-08-17T08:45:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rework1Id, "tech-kamal", "Kamal Technician", "2026-08-17T08:50:00Z", UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rework1Id, "Cyan plate cleaned and shifted 0.4mm", 200, "tech-kamal", "Kamal Technician", "First rework completed", "2026-08-17T09:30:00Z", UserRole.QC_INSPECTOR)

        // Return Rework 1 to QC
        val ret1Res = reworkRepository.returnToQc(rework1Id, "tech-kamal", "Kamal Technician", "Ready for Re-QC cycle 1", "2026-08-17T09:35:00Z", UserRole.QC_INSPECTOR)
        assertTrue(ret1Res is DomainResult.Success)
        assertEquals(ReworkStatus.RETURNED_TO_QC, (ret1Res as DomainResult.Success).data.status)

        // ==========================================
        // 4. Re-QC Cycle 1 Creation & Inspection -> FAIL
        // ==========================================
        val reqc1Res = reQcRepository.createReQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rework1Id,
            originalQcId = qcId,
            originalDefectId = defectId,
            affectedQuantity = 200,
            quantityUnit = "sheets",
            createdBy = "insp-tariq",
            createdByName = "Tariq Inspector",
            notes = "First recheck after plate alignment",
            timestamp = "2026-08-17T09:40:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(reqc1Res is DomainResult.Success)
        val reqc1 = (reqc1Res as DomainResult.Success).data
        val reqc1Id = reqc1.reQcId
        assertEquals(1, reqc1.cycleNumber)

        // Start inspection
        reQcRepository.startInspection(reqc1Id, "insp-tariq", "Tariq Inspector", timestamp = "2026-08-17T09:45:00Z", callerRole = UserRole.QC_INSPECTOR)

        // Record FAIL on Cycle 1
        val reqc1FailRes = reQcRepository.failReQc(
            reQcId = reqc1Id,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Cyan delta-E reduced to 1.8 but still exceeds 1.0 delta-E tolerance limit on 80 sheets",
            affectedQuantity = 80,
            quantityUnit = "sheets",
            failedItemIds = listOf("color-density-check-01"),
            inspectorId = "insp-tariq",
            inspectorName = "Tariq Inspector",
            nextAction = "Requires laser spectrometer recalibration and new plate burn",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(reqc1FailRes is DomainResult.Success)
        val reqc1Failed = (reqc1FailRes as DomainResult.Success).data
        assertEquals(ReQcStatus.FAILED, reqc1Failed.status)
        assertEquals(ReQcDecision.FAIL, reqc1Failed.decision)

        // Return Cycle 1 to rework
        val retReworkRes = reQcRepository.returnToRework(reqc1Id, "insp-tariq", "Tariq Inspector", "Sent back for second rework cycle", "2026-08-17T10:05:00Z", UserRole.QC_INSPECTOR)
        assertTrue(retReworkRes is DomainResult.Success)
        assertEquals(ReQcStatus.RETURNED_TO_REWORK, (retReworkRes as DomainResult.Success).data.status)

        // ==========================================
        // 5. Rework Cycle 2 Execution
        // ==========================================
        val rework2Res = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = qcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 80,
            quantityUnit = "sheets",
            description = "Reburn Cyan CTP plate with calibrated laser curves",
            requestedBy = "insp-tariq",
            requestedByName = "Tariq Inspector",
            timestamp = "2026-08-17T10:10:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(rework2Res is DomainResult.Success)
        val rework2Id = (rework2Res as DomainResult.Success).data.reworkId

        reworkRepository.startReview(rework2Id, "mgr-rahim", "Rahim Manager", "Approved expedited second rework", "2026-08-17T10:15:00Z", UserRole.MANAGER)
        reworkRepository.approveRework(rework2Id, "mgr-rahim", "Rahim Manager", "Immediate execution", "2026-08-17T10:16:00Z", UserRole.MANAGER)
        reworkRepository.assignRework(rework2Id, "tech-kamal", "Kamal Technician", "mgr-rahim", "Rahim Manager", "Priority fix", "2026-08-17T10:17:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rework2Id, "tech-kamal", "Kamal Technician", "2026-08-17T10:20:00Z", UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rework2Id, "New CTP plate mounted and laser profiled", 80, "tech-kamal", "Kamal Technician", "Second rework completed", "2026-08-17T10:50:00Z", UserRole.QC_INSPECTOR)
        reworkRepository.returnToQc(rework2Id, "tech-kamal", "Kamal Technician", "Ready for Re-QC cycle 2", "2026-08-17T10:55:00Z", UserRole.QC_INSPECTOR)

        // ==========================================
        // 6. Re-QC Cycle 2 Creation & Inspection -> PASS
        // ==========================================
        val reqc2Res = reQcRepository.createNextCycle(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rework2Id,
            previousReQcId = reqc1Id,
            originalQcId = qcId,
            originalDefectId = defectId,
            affectedQuantity = 80,
            quantityUnit = "sheets",
            createdBy = "insp-tariq",
            createdByName = "Tariq Inspector",
            notes = "Second recheck after new CTP plate mounting",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(reqc2Res is DomainResult.Success)
        val reqc2 = (reqc2Res as DomainResult.Success).data
        val reqc2Id = reqc2.reQcId
        assertEquals(2, reqc2.cycleNumber)
        assertEquals(reqc1Id, reqc2.previousReQcId)
        assertEquals(rework2Id, reqc2.productionReworkId)

        // Start inspection on Cycle 2
        reQcRepository.startInspection(reqc2Id, "insp-tariq", "Tariq Inspector", timestamp = "2026-08-17T11:05:00Z", callerRole = UserRole.QC_INSPECTOR)

        // PASS on Cycle 2
        val reqc2PassRes = reQcRepository.passReQc(
            reQcId = reqc2Id,
            inspectorId = "insp-tariq",
            inspectorName = "Tariq Inspector",
            passNotes = "Cyan delta-E measured at 0.3 across all 80 sheets. Excellent quality, approved.",
            timestamp = "2026-08-17T11:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(reqc2PassRes is DomainResult.Success)
        val reqc2Passed = (reqc2PassRes as DomainResult.Success).data
        assertEquals(ReQcStatus.PASSED, reqc2Passed.status)
        assertEquals(ReQcDecision.PASS, reqc2Passed.decision)
        assertTrue(reqc2Passed.isTerminal)

        // ==========================================
        // 7. Verify System Invariants
        // ==========================================
        // 1. Same productionJobId and projectId
        assertEquals(testJobId, reqc1.productionJobId)
        assertEquals(testJobId, reqc2.productionJobId)
        assertEquals(testProjectId, reqc1.projectId)
        assertEquals(testProjectId, reqc2.projectId)

        // 2. Lineage
        assertEquals(defectId, reqc1.originalDefectId)
        assertEquals(defectId, reqc2.originalDefectId)
        assertEquals(rework1Id, reqc1.productionReworkId)
        assertEquals(rework2Id, reqc2.productionReworkId)
        assertEquals(reqc1Id, reqc2.previousReQcId)

        // 3. Cycle progression: 1 -> 2
        val cycles = reQcRepository.observeReQcCycles(testJobId).first()
        assertEquals(2, cycles.size)
        assertEquals(1, cycles[0].cycleNumber)
        assertEquals(2, cycles[1].cycleNumber)
        assertEquals(ReQcStatus.RETURNED_TO_REWORK, cycles[0].status)
        assertEquals(ReQcStatus.PASSED, cycles[1].status)

        // 4. Immutable failure record preserved
        val failureHistory = reQcRepository.observeFailureHistory(productionJobId = testJobId).first()
        assertEquals(1, failureHistory.size)
        assertEquals(reqc1Id, failureHistory[0].reQcId)
        assertEquals(1, failureHistory[0].cycleNumber)
        assertEquals(ReQcFailureReason.DEFECT_REMAINS, failureHistory[0].failureReason)
        assertEquals(80, failureHistory[0].affectedQuantity)

        // 5. Audit trail preserved
        val cycle1Events = reQcRepository.observeReQcActivity(reqc1Id).first()
        assertTrue(cycle1Events.any { it.activityType == ReQcActivityType.RE_QC_CREATED })
        assertTrue(cycle1Events.any { it.activityType == ReQcActivityType.RE_QC_STARTED })
        assertTrue(cycle1Events.any { it.activityType == ReQcActivityType.RE_QC_FAILURE_RECORDED })
        assertTrue(cycle1Events.any { it.activityType == ReQcActivityType.RE_QC_FAILED })
        assertTrue(cycle1Events.any { it.activityType == ReQcActivityType.RE_QC_RETURNED_TO_REWORK })

        val cycle2Events = reQcRepository.observeReQcActivity(reqc2Id).first()
        assertTrue(cycle2Events.any { it.activityType == ReQcActivityType.RE_QC_CYCLE_CREATED })
        assertTrue(cycle2Events.any { it.activityType == ReQcActivityType.RE_QC_STARTED })
        assertTrue(cycle2Events.any { it.activityType == ReQcActivityType.RE_QC_PASSED })

        // 6. Immutability of terminal passed cycle
        val cannotModifyPassed = reQcRepository.failReQc(
            reQcId = reqc2Id,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Try to fail passed cycle",
            affectedQuantity = 10,
            inspectorId = "insp-tariq",
            timestamp = "2026-08-17T11:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        // Idempotency or error on terminal transition
        val current2 = (reQcRepository.findReQcById(reqc2Id) as DomainResult.Success).data
        assertEquals(ReQcStatus.PASSED, current2.status)
    }
}

package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityType
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseEligibility
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseStatus
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcType
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
 * Comprehensive End-to-End integration test covering the entire quality and release lifecycle:
 *
 * ProductionJob
 *     ↓
 * Pre-Production QC PASS
 *     ↓
 * Production QC
 *     ↓
 * Defect detected
 *     ↓
 * Rework 1 created & completed
 *     ↓
 * Returned to QC
 *     ↓
 * Re-QC Cycle 1 -> FAIL
 *     ↓
 * Failure Record stored
 *     ↓
 * Rework Cycle 2 created & completed
 *     ↓
 * Returned to QC
 *     ↓
 * Re-QC Cycle 2 -> PASS
 *     ↓
 * Final QC -> PASS
 *     ↓
 * Release Eligibility = ELIGIBLE
 *     ↓
 * Production Release Authorization -> RELEASED
 *
 * (Module 06 Step 07 Master Integration Suite).
 */
class FinalQcEndToEndIntegrationTest {

    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var finalQcDataSource: FakeFinalQcDataSource

    private lateinit var qcRepository: ProductionQcRepositoryImpl
    private lateinit var defectRepository: ProductionDefectRepositoryImpl
    private lateinit var reworkRepository: ProductionReworkRepositoryImpl
    private lateinit var reQcRepository: ProductionReQcRepositoryImpl
    private lateinit var finalQcRepository: FinalQcRepositoryImpl

    private val testProjectId = "proj-mag-900"
    private val testJobId = "job-card-900"

    @Before
    fun setUp() {
        jobDataSource = FakeProductionJobDataSource()
        qcDataSource = FakeProductionQcDataSource()
        defectDataSource = FakeProductionDefectDataSource()
        reworkDataSource = FakeProductionReworkDataSource()
        reQcDataSource = FakeProductionReQcDataSource()
        checklistDataSource = FakeQcChecklistDataSource()
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
    }

    @Test
    fun fullEndToEndQualityGateAndProductionReleaseWorkflow_succeeds() = runBlocking {
        // ==========================================
        // 1. Production Job Initialization
        // ==========================================
        val job = ProductionJob(
            jobId = testJobId,
            jobNumber = "JOB-900",
            orderId = testProjectId,
            orderNumber = "ORD-900",
            customerId = "cust-01",
            handoffId = "ho-900",
            title = "Deluxe Magazine Printing 5000 Qty",
            quantity = 5000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T07:00:00Z",
            updatedAt = "2026-08-17T07:00:00Z"
        )
        jobDataSource.insertJob(job)

        // ==========================================
        // 2. Pre-Production QC Inspection & PASS
        // ==========================================
        val preQcRes = qcRepository.createQc(
            productionJobId = testJobId,
            qcType = QcType.PRE_PRODUCTION,
            notes = "Pre-Production plate and paper stock check",
            timestamp = "2026-08-17T07:30:00Z"
        )
        val preQcId = (preQcRes as DomainResult.Success).data.qcId

        qcRepository.startInspection(preQcId, "insp-01", timestamp = "2026-08-17T07:35:00Z")
        val initItemsRes = qcRepository.initializePreProductionItems(preQcId, UserRole.QC_INSPECTOR)
        val items = (initItemsRes as DomainResult.Success).data
        items.forEach { item ->
            qcRepository.updatePreProductionItem(
                itemId = item.itemId,
                status = com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus.PASS,
                notes = "Verified",
                checkedBy = "insp-01",
                timestamp = "2026-08-17T07:45:00Z",
                callerRole = UserRole.QC_INSPECTOR
            )
        }

        val preQcPassRes = qcRepository.submitPreProductionQc(
            qcId = preQcId,
            decision = QcDecision.PASS,
            submittedBy = "insp-01",
            submittedByName = "Tariq Inspector",
            notes = "All 4-color plates and 150gsm paper stock verified.",
            timestamp = "2026-08-17T07:50:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(preQcPassRes is DomainResult.Success)

        // ==========================================
        // 3. Production QC & Defect Detection
        // ==========================================
        val inlineQcRes = qcRepository.createQc(
            productionJobId = testJobId,
            qcType = QcType.FINAL,
            notes = "In-line production inspection",
            timestamp = "2026-08-17T08:00:00Z"
        )
        val inlineQcId = (inlineQcRes as DomainResult.Success).data.qcId

        val defectRes = defectRepository.createDefect(
            productionJobId = testJobId,
            qcId = inlineQcId,
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Magenta Misregistration",
            description = "Magenta plate shifted 1.5mm on pages 12-13",
            affectedQuantity = 200,
            affectedUnit = "sheets",
            detectedBy = "insp-01",
            detectedByName = "Tariq Inspector",
            timestamp = "2026-08-17T08:15:00Z"
        )
        val defectId = (defectRes as DomainResult.Success).data.defectId
        defectRepository.acknowledgeDefect(defectId, "insp-01", timestamp = "2026-08-17T08:20:00Z")
        defectRepository.investigateDefect(defectId, "insp-01", timestamp = "2026-08-17T08:25:00Z")

        // ==========================================
        // 4. Rework 1 -> Returned to QC
        // ==========================================
        val rew1Res = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = inlineQcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 200,
            description = "Realign Magenta cylinder",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T08:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew1Id = (rew1Res as DomainResult.Success).data.reworkId

        reworkRepository.startReview(rew1Id, "mgr-01", timestamp = "2026-08-17T08:35:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.approveRework(rew1Id, "mgr-01", timestamp = "2026-08-17T08:40:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.assignRework(rew1Id, "tech-01", "Kamal Tech", "mgr-01", "Rahim", "Fix cylinder", "2026-08-17T08:45:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rew1Id, "tech-01", timestamp = "2026-08-17T08:50:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rew1Id, "Cylinder shifted", 200, "tech-01", timestamp = "2026-08-17T09:20:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.returnToQc(rew1Id, "tech-01", timestamp = "2026-08-17T09:25:00Z", callerRole = UserRole.QC_INSPECTOR)

        // ==========================================
        // 5. Re-QC Cycle 1 -> FAIL -> RETURNED_TO_REWORK
        // ==========================================
        val reqc1Res = reQcRepository.createReQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rew1Id,
            originalQcId = inlineQcId,
            originalDefectId = defectId,
            affectedQuantity = 200,
            createdBy = "insp-01",
            timestamp = "2026-08-17T09:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reqc1Id = (reqc1Res as DomainResult.Success).data.reQcId

        reQcRepository.startInspection(reqc1Id, "insp-01", timestamp = "2026-08-17T09:35:00Z", callerRole = UserRole.QC_INSPECTOR)
        reQcRepository.failReQc(
            reQcId = reqc1Id,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Magenta still shifted 0.6mm on 60 sheets",
            affectedQuantity = 60,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T09:50:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        reQcRepository.returnToRework(reqc1Id, "insp-01", timestamp = "2026-08-17T09:55:00Z", callerRole = UserRole.QC_INSPECTOR)

        // ==========================================
        // 6. Rework Cycle 2 -> Returned to QC
        // ==========================================
        val rew2Res = reworkRepository.createRework(
            projectId = testProjectId,
            productionJobId = testJobId,
            qcId = inlineQcId,
            defectId = defectId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 60,
            description = "Fine tune Magenta registration screws",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val rew2Id = (rew2Res as DomainResult.Success).data.reworkId

        reworkRepository.startReview(rew2Id, "mgr-01", timestamp = "2026-08-17T10:05:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.approveRework(rew2Id, "mgr-01", timestamp = "2026-08-17T10:06:00Z", callerRole = UserRole.MANAGER)
        reworkRepository.assignRework(rew2Id, "tech-01", "Kamal", "mgr-01", "Rahim", "Fine tune", "2026-08-17T10:07:00Z", UserRole.MANAGER)
        reworkRepository.startRework(rew2Id, "tech-01", timestamp = "2026-08-17T10:10:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.completeRework(rew2Id, "Re-calibrated registration", 60, "tech-01", timestamp = "2026-08-17T10:30:00Z", callerRole = UserRole.QC_INSPECTOR)
        reworkRepository.returnToQc(rew2Id, "tech-01", timestamp = "2026-08-17T10:35:00Z", callerRole = UserRole.QC_INSPECTOR)

        // ==========================================
        // 7. Re-QC Cycle 2 -> PASS
        // ==========================================
        val reqc2Res = reQcRepository.createNextCycle(
            projectId = testProjectId,
            productionJobId = testJobId,
            productionReworkId = rew2Id,
            previousReQcId = reqc1Id,
            originalQcId = inlineQcId,
            originalDefectId = defectId,
            affectedQuantity = 60,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:40:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reqc2Id = (reqc2Res as DomainResult.Success).data.reQcId

        reQcRepository.startInspection(reqc2Id, "insp-01", timestamp = "2026-08-17T10:45:00Z", callerRole = UserRole.QC_INSPECTOR)
        reQcRepository.passReQc(reqc2Id, "insp-01", "Tariq", "All 60 sheets pass 100% color registration.", timestamp = "2026-08-17T11:00:00Z", callerRole = UserRole.QC_INSPECTOR)

        // Mark defect as resolved now that Re-QC passed
        defectRepository.resolveDefect(defectId, "Color registration resolved after 2 rework passes", "insp-01", timestamp = "2026-08-17T11:05:00Z")

        // ==========================================
        // 8. Final QC Inspection -> PASS
        // ==========================================
        val finalQcRes = finalQcRepository.createFinalQc(
            projectId = testProjectId,
            productionJobId = testJobId,
            totalQuantity = 5000,
            preProductionQcId = preQcId,
            sourceDefectIds = listOf(defectId),
            sourceReworkIds = listOf(rew1Id, rew2Id),
            sourceReQcIds = listOf(reqc1Id, reqc2Id),
            notes = "Final comprehensive quality inspection for 5000 deluxe magazines.",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:10:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(finalQcRes is DomainResult.Success)
        val finalQcId = (finalQcRes as DomainResult.Success).data.finalQcId

        // Start Final QC
        finalQcRepository.startInspection(finalQcId, "insp-01", "Tariq Inspector", timestamp = "2026-08-17T11:15:00Z", callerRole = UserRole.QC_INSPECTOR)

        // Pass Final QC
        val passRes = finalQcRepository.submitPass(
            finalQcId = finalQcId,
            acceptedQuantity = 5000,
            notes = "5000 copies inspected. Clean binding, accurate trim, perfect color registration.",
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            timestamp = "2026-08-17T11:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(passRes is DomainResult.Success)

        // ==========================================
        // 9. Evaluate Release Eligibility -> ELIGIBLE
        // ==========================================
        val eligibilityRes = finalQcRepository.evaluateReleaseEligibility(finalQcId)
        assertTrue(eligibilityRes is DomainResult.Success)
        val eligibility = (eligibilityRes as DomainResult.Success).data
        assertTrue(eligibility.isEligible)
        assertEquals(listOf(FinalQcReleaseEligibility.ELIGIBLE), eligibility.reasons)

        // ==========================================
        // 10. Authorize Production Release -> RELEASED
        // ==========================================
        val releaseRes = finalQcRepository.authorizeProductionRelease(
            finalQcId = finalQcId,
            releaseNotes = "Approved for packaging and warehouse staging.",
            authorizedBy = "mgr-01",
            authorizedByName = "Rahim Manager",
            timestamp = "2026-08-17T11:45:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(releaseRes is DomainResult.Success)
        val auth = (releaseRes as DomainResult.Success).data
        assertNotNull(auth.releaseAuthorizationId)
        assertEquals(testJobId, auth.productionJobId)
        assertEquals(testProjectId, auth.projectId)
        assertEquals(FinalQcStatus.RELEASED, auth.finalQcStatus)
        assertEquals(FinalQcDecision.PASS, auth.finalQcDecision)

        // ==========================================
        // 11. Verification of System Invariants & Lineage
        // ==========================================
        val finalInspection = (finalQcRepository.findFinalQcById(finalQcId) as DomainResult.Success).data
        assertEquals(FinalQcStatus.RELEASED, finalInspection.status)
        assertEquals(FinalQcReleaseStatus.AUTHORIZED, finalInspection.releaseStatus)
        assertEquals(auth.releaseAuthorizationId, finalInspection.releaseAuthorizationId)
        assertTrue(finalInspection.isReleased)
        assertTrue(finalInspection.isTerminal)

        // Audit Trail Completeness
        val auditEvents = finalQcRepository.observeFinalQcActivity(finalQcId).first()
        val types = auditEvents.map { it.activityType }
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_CREATED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_STARTED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_PASSED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_RELEASE_ELIGIBILITY_CHECKED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_RELEASE_AUTHORIZED))
    }
}

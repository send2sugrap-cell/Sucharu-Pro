package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for multi-cycle Failure Loop execution and historical preservation (Module 06 Step 06).
 */
class ReQcFailureLoopTest {

    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReQcRepository

    @Before
    fun setUp() {
        runBlocking {
            reQcDataSource = FakeProductionReQcDataSource()
            reworkDataSource = FakeProductionReworkDataSource()
            repository = ProductionReQcRepositoryImpl(
                reQcDataSource = reQcDataSource,
                reworkDataSource = reworkDataSource
            )
        }
    }

    @Test
    fun multiCycleFailureLoop_executesSuccessfully_andPreservesHistory() = runBlocking {
        // Step 1: Initial Rework 1 in RETURNED_TO_QC
        reworkDataSource.insertRework(
            ProductionRework(
                reworkId = "rew-01",
                projectId = "proj-01",
                productionJobId = "job-01",
                reworkType = ReworkType.PRINT_CORRECTION,
                reason = ReworkReason.PRINT_ERROR,
                status = ReworkStatus.RETURNED_TO_QC,
                affectedQuantity = 50,
                description = "Fix print ink smudges",
                requestedBy = "user-01",
                requestedAt = "2026-08-17T09:00:00Z",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        // Step 2: Create Re-QC Cycle 1
        val c1Res = repository.createReQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            productionReworkId = "rew-01",
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(c1Res is DomainResult.Success)
        val cycle1Id = (c1Res as DomainResult.Success).data.reQcId
        assertEquals(1, (c1Res as DomainResult.Success).data.cycleNumber)

        // Cycle 1 Inspection & FAIL
        repository.startInspection(cycle1Id, "insp-01", timestamp = "2026-08-17T10:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        val c1FailRes = repository.failReQc(
            reQcId = cycle1Id,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Smudge still present on margin.",
            affectedQuantity = 20,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(c1FailRes is DomainResult.Success)

        // Return Cycle 1 to rework
        repository.returnToRework(cycle1Id, "insp-01", timestamp = "2026-08-17T10:20:00Z", callerRole = UserRole.QC_INSPECTOR)

        // Step 3: Second Rework 2 in RETURNED_TO_QC
        reworkDataSource.insertRework(
            ProductionRework(
                reworkId = "rew-02",
                projectId = "proj-01",
                productionJobId = "job-01",
                reworkType = ReworkType.PRINT_CORRECTION,
                reason = ReworkReason.DEFECT_CORRECTION,
                status = ReworkStatus.RETURNED_TO_QC,
                affectedQuantity = 20,
                description = "Second pass cleaning and reprint",
                requestedBy = "user-01",
                requestedAt = "2026-08-17T10:30:00Z",
                createdAt = "2026-08-17T10:30:00Z",
                updatedAt = "2026-08-17T10:30:00Z"
            )
        )

        // Step 4: Create Re-QC Cycle 2 linked to Cycle 1
        val c2Res = repository.createNextCycle(
            projectId = "proj-01",
            productionJobId = "job-01",
            productionReworkId = "rew-02",
            previousReQcId = cycle1Id,
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(c2Res is DomainResult.Success)
        val cycle2 = (c2Res as DomainResult.Success).data
        assertEquals(2, cycle2.cycleNumber)
        assertEquals(cycle1Id, cycle2.previousReQcId)
        assertEquals("rew-02", cycle2.productionReworkId)

        // Cycle 2 Inspection & PASS
        repository.startInspection(cycle2.reQcId, "insp-01", timestamp = "2026-08-17T11:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        val c2PassRes = repository.passReQc(
            reQcId = cycle2.reQcId,
            inspectorId = "insp-01",
            passNotes = "All smudges cleared. Perfect print registration.",
            timestamp = "2026-08-17T11:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(c2PassRes is DomainResult.Success)
        val cycle2Passed = (c2PassRes as DomainResult.Success).data
        assertEquals(ReQcStatus.PASSED, cycle2Passed.status)
        assertEquals(ReQcDecision.PASS, cycle2Passed.decision)

        // Verify full cycle history
        val cycles = repository.observeReQcCycles("job-01").first()
        assertEquals(2, cycles.size)
        assertEquals(1, cycles[0].cycleNumber)
        assertEquals(ReQcStatus.RETURNED_TO_REWORK, cycles[0].status)
        assertEquals(2, cycles[1].cycleNumber)
        assertEquals(ReQcStatus.PASSED, cycles[1].status)

        // Verify failure history preserved
        val failureRecords = repository.observeFailureHistory(productionJobId = "job-01").first()
        assertEquals(1, failureRecords.size)
        assertEquals(cycle1Id, failureRecords[0].reQcId)
        assertEquals(1, failureRecords[0].cycleNumber)
    }
}

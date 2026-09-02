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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for failing Re-QC inspection and failure record generation (Module 06 Step 06).
 */
class ReQcFailureTest {

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

            reworkDataSource.insertRework(
                ProductionRework(
                    reworkId = "rew-001",
                    projectId = "proj-001",
                    productionJobId = "job-001",
                    reworkType = ReworkType.COLOR_CORRECTION,
                    reason = ReworkReason.DEFECT_CORRECTION,
                    status = ReworkStatus.RETURNED_TO_QC,
                    affectedQuantity = 100,
                    description = "Color fixed",
                    requestedBy = "user-01",
                    requestedAt = "2026-08-17T10:00:00Z",
                    createdAt = "2026-08-17T10:00:00Z",
                    updatedAt = "2026-08-17T10:00:00Z"
                )
            )
        }
    }

    @Test
    fun failReQc_createsImmutableFailureRecord_andTransitionsToFailed() = runBlocking {
        val createRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (createRes as DomainResult.Success).data.reQcId

        repository.startInspection(
            reQcId = reQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            timestamp = "2026-08-17T11:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val failRes = repository.failReQc(
            reQcId = reQcId,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Color banding still visible on spine section.",
            affectedQuantity = 40,
            quantityUnit = "pcs",
            failedItemIds = listOf("item-spine-01"),
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            nextAction = "Requires second pass alignment",
            timestamp = "2026-08-17T11:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(failRes is DomainResult.Success)
        val failed = (failRes as DomainResult.Success).data
        assertEquals(ReQcStatus.FAILED, failed.status)
        assertEquals(ReQcDecision.FAIL, failed.decision)
        assertEquals(ReQcFailureReason.DEFECT_REMAINS, failed.failureReason)
        assertEquals(40, failed.affectedQuantity)

        // Verify failure record was created in datasource
        val failureRecords = repository.observeFailureHistory(reQcId = reQcId).first()
        assertEquals(1, failureRecords.size)
        val record = failureRecords[0]
        assertNotNull(record.failureRecordId)
        assertEquals(reQcId, record.reQcId)
        assertEquals(1, record.cycleNumber)
        assertEquals(ReQcFailureReason.DEFECT_REMAINS, record.failureReason)
        assertEquals("Color banding still visible on spine section.", record.failureNotes)
        assertEquals(40, record.affectedQuantity)
        assertEquals("insp-01", record.detectedBy)
    }

    @Test
    fun returnToRework_transitionsFailedToReturnedToRework() = runBlocking {
        val createRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (createRes as DomainResult.Success).data.reQcId

        repository.startInspection(
            reQcId = reQcId,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T11:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        repository.failReQc(
            reQcId = reQcId,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Defect persists",
            affectedQuantity = 20,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T11:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val returnRes = repository.returnToRework(
            reQcId = reQcId,
            actorId = "insp-01",
            actorName = "Tariq Inspector",
            notes = "Returned to rework queue",
            timestamp = "2026-08-17T11:40:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(returnRes is DomainResult.Success)
        val reQc = (returnRes as DomainResult.Success).data
        assertEquals(ReQcStatus.RETURNED_TO_REWORK, reQc.status)
        assertEquals("2026-08-17T11:40:00Z", reQc.returnedToReworkAt)
    }
}

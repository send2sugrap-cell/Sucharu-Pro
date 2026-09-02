package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for cycle history retrieval and cycle ordering (Module 06 Step 06).
 */
class ReQcCycleHistoryTest {

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
    fun observeReQcCycles_orderedByCycleNumber() = runBlocking {
        // Seed rework
        reworkDataSource.insertRework(
            ProductionRework(
                reworkId = "rew-01",
                projectId = "proj-01",
                productionJobId = "job-01",
                reworkType = ReworkType.COLOR_CORRECTION,
                reason = ReworkReason.DEFECT_CORRECTION,
                status = ReworkStatus.RETURNED_TO_QC,
                affectedQuantity = 50,
                description = "Rework 01",
                requestedBy = "user-01",
                requestedAt = "2026-08-17T09:00:00Z",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val c1 = (repository.createReQc("proj-01", "job-01", "rew-01", createdBy = "insp-01", timestamp = "2026-08-17T10:00:00Z", callerRole = UserRole.QC_INSPECTOR) as DomainResult.Success).data
        repository.startInspection(c1.reQcId, "insp-01", timestamp = "2026-08-17T10:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.failReQc(c1.reQcId, ReQcFailureReason.DEFECT_REMAINS, "Fail 1", 10, inspectorId = "insp-01", timestamp = "2026-08-17T10:10:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.returnToRework(c1.reQcId, "insp-01", timestamp = "2026-08-17T10:15:00Z", callerRole = UserRole.QC_INSPECTOR)

        reworkDataSource.insertRework(
            ProductionRework(
                reworkId = "rew-02",
                projectId = "proj-01",
                productionJobId = "job-01",
                reworkType = ReworkType.COLOR_CORRECTION,
                reason = ReworkReason.DEFECT_CORRECTION,
                status = ReworkStatus.RETURNED_TO_QC,
                affectedQuantity = 10,
                description = "Rework 02",
                requestedBy = "user-01",
                requestedAt = "2026-08-17T10:20:00Z",
                createdAt = "2026-08-17T10:20:00Z",
                updatedAt = "2026-08-17T10:20:00Z"
            )
        )

        val c2 = (repository.createNextCycle("proj-01", "job-01", "rew-02", previousReQcId = c1.reQcId, createdBy = "insp-01", timestamp = "2026-08-17T10:30:00Z", callerRole = UserRole.QC_INSPECTOR) as DomainResult.Success).data

        val history = repository.observeReQcCycles("job-01").first()
        assertEquals(2, history.size)
        assertEquals(1, history[0].cycleNumber)
        assertEquals(2, history[1].cycleNumber)

        val latest = (repository.getLatestReQcCycle("job-01") as DomainResult.Success).data
        assertNotNull(latest)
        assertEquals(2, latest?.cycleNumber)
    }
}

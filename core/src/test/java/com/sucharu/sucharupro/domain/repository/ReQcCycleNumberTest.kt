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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying strictly increasing cycle numbering across cycles (Module 06 Step 06).
 */
class ReQcCycleNumberTest {

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

    private suspend fun createAndReturnRework(reworkId: String) {
        reworkDataSource.insertRework(
            ProductionRework(
                reworkId = reworkId,
                projectId = "proj-01",
                productionJobId = "job-01",
                reworkType = ReworkType.COLOR_CORRECTION,
                reason = ReworkReason.DEFECT_CORRECTION,
                status = ReworkStatus.RETURNED_TO_QC,
                affectedQuantity = 50,
                description = "Rework $reworkId",
                requestedBy = "user-01",
                requestedAt = "2026-08-17T09:00:00Z",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )
    }

    @Test
    fun cycleNumbers_incrementStrictly() = runBlocking {
        // Cycle 1
        createAndReturnRework("rew-01")
        val c1 = (repository.createReQc("proj-01", "job-01", "rew-01", createdBy = "insp-01", timestamp = "2026-08-17T10:00:00Z", callerRole = UserRole.QC_INSPECTOR) as DomainResult.Success).data
        assertEquals(1, c1.cycleNumber)

        repository.startInspection(c1.reQcId, "insp-01", timestamp = "2026-08-17T10:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.failReQc(c1.reQcId, ReQcFailureReason.DEFECT_REMAINS, "Fail 1", 10, inspectorId = "insp-01", timestamp = "2026-08-17T10:10:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.returnToRework(c1.reQcId, "insp-01", timestamp = "2026-08-17T10:15:00Z", callerRole = UserRole.QC_INSPECTOR)

        // Cycle 2
        createAndReturnRework("rew-02")
        val c2 = (repository.createNextCycle("proj-01", "job-01", "rew-02", previousReQcId = c1.reQcId, createdBy = "insp-01", timestamp = "2026-08-17T10:20:00Z", callerRole = UserRole.QC_INSPECTOR) as DomainResult.Success).data
        assertEquals(2, c2.cycleNumber)

        repository.startInspection(c2.reQcId, "insp-01", timestamp = "2026-08-17T10:25:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.failReQc(c2.reQcId, ReQcFailureReason.REWORK_INCOMPLETE, "Fail 2", 10, inspectorId = "insp-01", timestamp = "2026-08-17T10:30:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.returnToRework(c2.reQcId, "insp-01", timestamp = "2026-08-17T10:35:00Z", callerRole = UserRole.QC_INSPECTOR)

        // Cycle 3
        createAndReturnRework("rew-03")
        val c3 = (repository.createNextCycle("proj-01", "job-01", "rew-03", previousReQcId = c2.reQcId, createdBy = "insp-01", timestamp = "2026-08-17T10:40:00Z", callerRole = UserRole.QC_INSPECTOR) as DomainResult.Success).data
        assertEquals(3, c3.cycleNumber)
    }
}

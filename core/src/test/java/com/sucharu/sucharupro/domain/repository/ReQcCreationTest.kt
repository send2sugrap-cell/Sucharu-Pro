package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReQcCycleType
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
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
 * Tests for Re-QC creation and initial cycle registration (Module 06 Step 06).
 */
class ReQcCreationTest {

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

            // Seed a valid rework in RETURNED_TO_QC status
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
    fun createReQc_validParameters_createsPendingReQcCycle1() = runBlocking {
        val result = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            cycleType = ReQcCycleType.POST_REWORK,
            affectedQuantity = 100,
            quantityUnit = "pcs",
            createdBy = "insp-01",
            createdByName = "Tariq Inspector",
            notes = "Post-rework verification",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val reQc = (result as DomainResult.Success).data
        assertNotNull(reQc.reQcId)
        assertEquals("job-001", reQc.productionJobId)
        assertEquals("proj-001", reQc.projectId)
        assertEquals("rew-001", reQc.productionReworkId)
        assertEquals(1, reQc.cycleNumber)
        assertEquals(ReQcStatus.PENDING, reQc.status)
        assertEquals(ReQcDecision.PENDING, reQc.decision)
        assertEquals(100, reQc.affectedQuantity)

        val list = repository.observeReQcList().first()
        assertEquals(1, list.size)
        assertEquals(reQc.reQcId, list[0].reQcId)
    }

    @Test
    fun createReQc_invalidReworkStatus_fails() = runBlocking {
        reworkDataSource.insertRework(
            ProductionRework(
                reworkId = "rew-002",
                projectId = "proj-001",
                productionJobId = "job-001",
                reworkType = ReworkType.COLOR_CORRECTION,
                reason = ReworkReason.DEFECT_CORRECTION,
                status = ReworkStatus.IN_PROGRESS,
                affectedQuantity = 50,
                description = "In progress",
                requestedBy = "user-01",
                requestedAt = "2026-08-17T10:00:00Z",
                createdAt = "2026-08-17T10:00:00Z",
                updatedAt = "2026-08-17T10:00:00Z"
            )
        )

        val result = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-002",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("must be in 'RETURNED_TO_QC' status"))
    }
}

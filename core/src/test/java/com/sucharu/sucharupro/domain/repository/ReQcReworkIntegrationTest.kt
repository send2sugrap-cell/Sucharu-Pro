package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
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
 * Tests for rework integration and tracking by rework ID with Step 05 (Module 06 Step 06).
 */
class ReQcReworkIntegrationTest {

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
    fun observeReQcByRework_retrievesLinkedReQc() = runBlocking {
        val result = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Success)

        val list = repository.observeReQcByRework("rew-001").first()
        assertEquals(1, list.size)
        assertEquals("rew-001", list[0].productionReworkId)
    }
}

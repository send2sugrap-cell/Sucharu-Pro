package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying the controlled Return-to-QC handoff boundary for subsequent Re-QC (Module 06 Step 05).
 */
class ProductionReworkReturnToQcTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun returnToQc_fromCompleted_succeedsAndSetsHandoffBoundary() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Color correction",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)
        repository.assignRework(reworkId, "tech-01", "Rahim", "mgr-01", "Manager", null, "2026-08-17T10:20:00Z", UserRole.MANAGER)
        repository.startRework(reworkId, "tech-01", "Rahim", "2026-08-17T10:30:00Z", UserRole.QC_INSPECTOR)
        repository.completeRework(
            reworkId = reworkId,
            correctiveAction = "Re-inked fountain rollers",
            actualReworkedQuantity = 100,
            completedBy = "tech-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // Hand off to QC
        val returnRes = repository.returnToQc(
            reworkId = reworkId,
            returnedBy = "tech-01",
            returnedByName = "Rahim",
            notes = "Ready for Re-QC inspection",
            timestamp = "2026-08-17T11:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(returnRes is DomainResult.Success)
        val returned = (returnRes as DomainResult.Success).data
        assertEquals(ReworkStatus.RETURNED_TO_QC, returned.status)
        assertNotNull(returned.returnedToQcAt)
        assertTrue(returned.isReturnedToQc)
    }

    @Test
    fun returnToQc_fromNonCompleted_fails() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Color correction",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        val returnRes = repository.returnToQc(
            reworkId = reworkId,
            returnedBy = "tech-01",
            timestamp = "2026-08-17T11:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(returnRes is DomainResult.Error)
        val error = returnRes as DomainResult.Error
        assertTrue(error.message.contains("not in COMPLETED status"))
    }
}

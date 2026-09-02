package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying strict quantity controls on rework creation and completion (Module 06 Step 05).
 */
class ProductionReworkQuantityTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun completion_actualQuantityLessThanOrEqualToAffected_succeeds() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.CUTTING_CORRECTION,
            reason = ReworkReason.FAILED_QC,
            affectedQuantity = 100,
            quantityUnit = "pcs",
            description = "Trim adjustment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        // Move to APPROVED -> ASSIGNED -> IN_PROGRESS
        repository.approveRework(reworkId, "admin-01", "Admin", null, "2026-08-17T10:10:00Z", UserRole.ADMIN)
        repository.assignRework(reworkId, "tech-01", "Rahim", "admin-01", "Admin", null, "2026-08-17T10:20:00Z", UserRole.ADMIN)
        repository.startRework(reworkId, "tech-01", "Rahim", "2026-08-17T10:30:00Z", UserRole.QC_INSPECTOR)

        // Complete with 95 pcs (valid)
        val completeRes = repository.completeRework(
            reworkId = reworkId,
            correctiveAction = "Re-trimmed edges with calibrated blade",
            actualReworkedQuantity = 95,
            completedBy = "tech-01",
            completedByName = "Rahim",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(completeRes is DomainResult.Success)
        val completed = (completeRes as DomainResult.Success).data
        assertEquals(95, completed.actualReworkedQuantity)
    }

    @Test
    fun completion_actualQuantityExceedingAffected_fails() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.CUTTING_CORRECTION,
            reason = ReworkReason.FAILED_QC,
            affectedQuantity = 100,
            quantityUnit = "pcs",
            description = "Trim adjustment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        repository.approveRework(reworkId, "admin-01", "Admin", null, "2026-08-17T10:10:00Z", UserRole.ADMIN)
        repository.assignRework(reworkId, "tech-01", "Rahim", "admin-01", "Admin", null, "2026-08-17T10:20:00Z", UserRole.ADMIN)
        repository.startRework(reworkId, "tech-01", "Rahim", "2026-08-17T10:30:00Z", UserRole.QC_INSPECTOR)

        // Complete with 150 pcs (exceeds 100)
        val completeRes = repository.completeRework(
            reworkId = reworkId,
            correctiveAction = "Re-trimmed",
            actualReworkedQuantity = 150,
            completedBy = "tech-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(completeRes is DomainResult.Error)
        val error = completeRes as DomainResult.Error
        assertTrue(error.message.contains("cannot exceed affected quantity"))
    }

    @Test
    fun completion_negativeActualQuantity_fails() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.CUTTING_CORRECTION,
            reason = ReworkReason.FAILED_QC,
            affectedQuantity = 100,
            quantityUnit = "pcs",
            description = "Trim adjustment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        repository.approveRework(reworkId, "admin-01", "Admin", null, "2026-08-17T10:10:00Z", UserRole.ADMIN)
        repository.assignRework(reworkId, "tech-01", "Rahim", "admin-01", "Admin", null, "2026-08-17T10:20:00Z", UserRole.ADMIN)
        repository.startRework(reworkId, "tech-01", "Rahim", "2026-08-17T10:30:00Z", UserRole.QC_INSPECTOR)

        val completeRes = repository.completeRework(
            reworkId = reworkId,
            correctiveAction = "Re-trimmed",
            actualReworkedQuantity = -5,
            completedBy = "tech-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(completeRes is DomainResult.Error)
        val error = completeRes as DomainResult.Error
        assertTrue(error.message.contains("cannot be negative"))
    }
}

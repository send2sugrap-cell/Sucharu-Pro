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
 * Tests for Rework execution and completion lifecycle milestones (Module 06 Step 05).
 */
class ProductionReworkCompletionTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun completeRework_withCorrectiveActionAndQuantity_succeeds() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.BINDING_CORRECTION,
            reason = ReworkReason.FINISHING_ERROR,
            affectedQuantity = 50,
            quantityUnit = "books",
            description = "Perfect binding spine glue cracking",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)
        repository.assignRework(reworkId, "tech-01", "Binder Master", "mgr-01", "Manager", null, "2026-08-17T10:20:00Z", UserRole.MANAGER)
        repository.startRework(reworkId, "tech-01", "Binder Master", "2026-08-17T10:30:00Z", UserRole.QC_INSPECTOR)

        val completeRes = repository.completeRework(
            reworkId = reworkId,
            correctiveAction = "Re-melted spine PUR glue and clamped under 180C pressure for 45s",
            actualReworkedQuantity = 50,
            completedBy = "tech-01",
            completedByName = "Binder Master",
            notes = "All 50 copies re-bound and spine pull-tested",
            timestamp = "2026-08-17T11:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(completeRes is DomainResult.Success)
        val completed = (completeRes as DomainResult.Success).data
        assertEquals(ReworkStatus.COMPLETED, completed.status)
        assertEquals(50, completed.actualReworkedQuantity)
        assertEquals("Re-melted spine PUR glue and clamped under 180C pressure for 45s", completed.correctiveAction)
        assertNotNull(completed.completedAt)
        assertTrue(completed.isCompleted)
    }

    @Test
    fun completeRework_withoutStarting_fails() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.BINDING_CORRECTION,
            reason = ReworkReason.FINISHING_ERROR,
            affectedQuantity = 50,
            quantityUnit = "books",
            description = "Spine cracking",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId
        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)
        repository.assignRework(reworkId, "tech-01", "Binder Master", "mgr-01", "Manager", null, "2026-08-17T10:20:00Z", UserRole.MANAGER)
        // Not started yet (status is ASSIGNED)

        val completeRes = repository.completeRework(
            reworkId = reworkId,
            correctiveAction = "Fixed",
            actualReworkedQuantity = 50,
            completedBy = "tech-01",
            timestamp = "2026-08-17T11:45:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(completeRes is DomainResult.Error)
        val error = completeRes as DomainResult.Error
        assertTrue(error.message.contains("not in IN_PROGRESS status"))
    }
}

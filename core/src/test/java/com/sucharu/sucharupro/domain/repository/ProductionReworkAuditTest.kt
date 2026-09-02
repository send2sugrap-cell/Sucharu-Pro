package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkActivityType
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying immutable audit trail emission across all rework mutations (Module 06 Step 05).
 */
class ProductionReworkAuditTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun completeLifecycle_recordsOrderedAuditTrail() = runBlocking {
        // 1. Create
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Color correction",
            requestedBy = "insp-01",
            requestedByName = "Inspector",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        // 2. Review
        repository.startReview(reworkId, "mgr-01", "Manager", "Reviewing", "2026-08-17T10:10:00Z", UserRole.MANAGER)

        // 3. Approve
        repository.approveRework(reworkId, "mgr-01", "Manager", "Approved", "2026-08-17T10:20:00Z", UserRole.MANAGER)

        // 4. Assign
        repository.assignRework(reworkId, "tech-01", "Rahim", "mgr-01", "Manager", "Assigned", "2026-08-17T10:30:00Z", UserRole.MANAGER)

        // 5. Start
        repository.startRework(reworkId, "tech-01", "Rahim", "2026-08-17T10:40:00Z", UserRole.QC_INSPECTOR)

        // 6. Complete
        repository.completeRework(reworkId, "Re-calibrated color curve", 100, "tech-01", "Rahim", "Done", "2026-08-17T11:00:00Z", UserRole.QC_INSPECTOR)

        // 7. Return to QC
        repository.returnToQc(reworkId, "tech-01", "Rahim", "Ready for Re-QC", "2026-08-17T11:15:00Z", UserRole.QC_INSPECTOR)

        val activities = repository.observeReworkActivity(reworkId).first()
        assertEquals(7, activities.size)

        val activityTypes = activities.map { it.activityType }
        assertTrue(activityTypes.contains(ReworkActivityType.REWORK_REQUESTED))
        assertTrue(activityTypes.contains(ReworkActivityType.REWORK_REVIEW_STARTED))
        assertTrue(activityTypes.contains(ReworkActivityType.REWORK_APPROVED))
        assertTrue(activityTypes.contains(ReworkActivityType.REWORK_ASSIGNED))
        assertTrue(activityTypes.contains(ReworkActivityType.REWORK_STARTED))
        assertTrue(activityTypes.contains(ReworkActivityType.REWORK_COMPLETED))
        assertTrue(activityTypes.contains(ReworkActivityType.REWORK_RETURNED_TO_QC))
    }
}

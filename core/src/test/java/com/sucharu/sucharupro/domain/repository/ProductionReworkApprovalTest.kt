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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Rework review, approval, and rejection workflow (Module 06 Step 05).
 */
class ProductionReworkApprovalTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun startReviewAndApprove_byManager_succeeds() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Color balance adjustment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        // Start review by manager
        val reviewRes = repository.startReview(
            reworkId = reworkId,
            reviewerId = "mgr-01",
            reviewerName = "Manager Kamal",
            notes = "Feasibility verified with press operator",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(reviewRes is DomainResult.Success)
        val underReview = (reviewRes as DomainResult.Success).data
        assertEquals(ReworkStatus.UNDER_REVIEW, underReview.status)
        assertEquals("mgr-01", underReview.reviewedBy)

        // Approve by manager
        val approveRes = repository.approveRework(
            reworkId = reworkId,
            approvedBy = "mgr-01",
            approvedByName = "Manager Kamal",
            notes = "Approved for 100 sheets reprint/color run",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data
        assertEquals(ReworkStatus.APPROVED, approved.status)
    }

    @Test
    fun rejectRework_withReason_transitionsToRejected() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Color balance adjustment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        val rejectRes = repository.rejectRework(
            reworkId = reworkId,
            reason = "Client accepted minor shade variance within delta-E tolerance",
            rejectedBy = "mgr-01",
            rejectedByName = "Manager Kamal",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data
        assertEquals(ReworkStatus.REJECTED, rejected.status)
        assertTrue(rejected.isTerminal)
    }

    @Test
    fun approveRework_byUnauthorizedInspector_failsSeparationOfDuties() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Color balance adjustment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        val approveRes = repository.approveRework(
            reworkId = reworkId,
            approvedBy = "insp-01",
            approvedByName = "Tariq Inspector",
            notes = "Self-approval attempt",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR // Unauthorized for approval
        )
        assertTrue(approveRes is DomainResult.Error)
        val error = approveRes as DomainResult.Error
        assertTrue(error.message.contains("Requires Admin or Manager"))
    }
}

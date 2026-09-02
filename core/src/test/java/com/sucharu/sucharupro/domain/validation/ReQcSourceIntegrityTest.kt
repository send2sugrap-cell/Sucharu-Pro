package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for source rework status and integrity validation (Module 06 Step 06).
 */
class ReQcSourceIntegrityTest {

    private fun createRework(status: ReworkStatus, jobId: String = "job-001"): ProductionRework {
        return ProductionRework(
            reworkId = "rew-001",
            projectId = "proj-001",
            productionJobId = jobId,
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            status = status,
            affectedQuantity = 100,
            description = "Color correction",
            requestedBy = "user-001",
            requestedAt = "2026-08-17T10:00:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun validateSourceRework_returnedToQc_isSuccess() {
        val rework = createRework(ReworkStatus.RETURNED_TO_QC)
        val result = ReQcValidator.validateSourceRework("job-001", rework)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateSourceRework_inProgressStatus_fails() {
        val rework = createRework(ReworkStatus.IN_PROGRESS)
        val result = ReQcValidator.validateSourceRework("job-001", rework)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("must be in 'RETURNED_TO_QC' status"))
    }

    @Test
    fun validateSourceRework_completedNotYetReturned_fails() {
        val rework = createRework(ReworkStatus.COMPLETED)
        val result = ReQcValidator.validateSourceRework("job-001", rework)
        assertTrue(result is DomainResult.Error)
    }
}

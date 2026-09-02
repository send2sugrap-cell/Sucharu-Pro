package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests enforcing cross-project isolation for Re-QC (Module 06 Step 06).
 */
class ReQcCrossProjectIsolationTest {

    @Test
    fun reworkCrossProject_rejected() {
        val rework = ProductionRework(
            reworkId = "rew-001",
            projectId = "proj-B",
            productionJobId = "job-001",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            status = ReworkStatus.RETURNED_TO_QC,
            affectedQuantity = 50,
            description = "Fix color",
            requestedBy = "user-01",
            requestedAt = "2026-08-17T10:00:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = ReQcValidator.validateReworkCrossProjectIsolation("proj-A", rework)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-project reference violation"))
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Terminal state protection and immutability tests for Final QC (Module 06 Step 07).
 */
class FinalQcTerminalStateTest {

    @Test
    fun releasedStatus_isTerminal() {
        val inspection = FinalQcInspection(
            finalQcId = "fqc-01",
            projectId = "proj-01",
            productionJobId = "job-01",
            status = FinalQcStatus.RELEASED,
            totalQuantity = 100,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(inspection.isTerminal)
        assertTrue(FinalQcLifecycleValidator.validateTerminalImmutability(inspection) is DomainResult.Error)
    }

    @Test
    fun cancelledStatus_isTerminal() {
        val inspection = FinalQcInspection(
            finalQcId = "fqc-02",
            projectId = "proj-01",
            productionJobId = "job-01",
            status = FinalQcStatus.CANCELLED,
            totalQuantity = 100,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(inspection.isTerminal)
        assertTrue(FinalQcLifecycleValidator.validateTerminalImmutability(inspection) is DomainResult.Error)
    }
}

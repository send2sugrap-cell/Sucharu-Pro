package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for terminal state immutability in Re-QC (Module 06 Step 06).
 */
class ReQcTerminalStateTest {

    private fun createSampleReQc(status: ReQcStatus): ReQcInspection {
        return ReQcInspection(
            reQcId = "reqc-001",
            productionJobId = "job-001",
            projectId = "proj-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            status = status,
            createdBy = "user-001",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun passedStatus_isTerminal_andNotEditable() {
        val reQc = createSampleReQc(ReQcStatus.PASSED)
        assertTrue(reQc.isTerminal)
        assertFalse(reQc.isEditable)
    }

    @Test
    fun cancelledStatus_isTerminal_andNotEditable() {
        val reQc = createSampleReQc(ReQcStatus.CANCELLED)
        assertTrue(reQc.isTerminal)
        assertFalse(reQc.isEditable)
    }

    @Test
    fun terminalReQc_cannotBeCancelledAgain() {
        val reQc = createSampleReQc(ReQcStatus.PASSED)
        val result = ReQcLifecycleValidator.validateCancellation(reQc, "Try cancel")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cannot cancel already terminal"))
    }
}

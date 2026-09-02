package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ReQcLifecycleValidator] lifecycle state machine and transition rules (Module 06 Step 06).
 */
class ReQcLifecycleTest {

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
    fun validateStatusTransition_pendingToAssigned_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.PENDING)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.ASSIGNED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateStatusTransition_assignedToInInspection_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.ASSIGNED)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.IN_INSPECTION)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateStatusTransition_inInspectionToPassed_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.IN_INSPECTION)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.PASSED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateStatusTransition_inInspectionToFailed_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.IN_INSPECTION)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.FAILED)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateStatusTransition_failedToReturnedToRework_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.FAILED)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.RETURNED_TO_REWORK)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateStatusTransition_passedToAnything_fails() {
        val reQc = createSampleReQc(ReQcStatus.PASSED)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.IN_INSPECTION)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal"))
    }

    @Test
    fun validateStatusTransition_cancelledToAnything_fails() {
        val reQc = createSampleReQc(ReQcStatus.CANCELLED)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.PENDING)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal"))
    }

    @Test
    fun validateStatusTransition_returnedToReworkToAnything_fails() {
        val reQc = createSampleReQc(ReQcStatus.RETURNED_TO_REWORK)
        val result = ReQcLifecycleValidator.validateStatusTransition(reQc, ReQcStatus.IN_INSPECTION)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("returned to rework"))
    }

    @Test
    fun validateInspectionStart_fromPending_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.PENDING)
        val result = ReQcLifecycleValidator.validateInspectionStart(reQc, "insp-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateInspectionStart_fromPassed_fails() {
        val reQc = createSampleReQc(ReQcStatus.PASSED)
        val result = ReQcLifecycleValidator.validateInspectionStart(reQc, "insp-01")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validatePassTransition_fromInInspection_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.IN_INSPECTION)
        val result = ReQcLifecycleValidator.validatePassTransition(reQc)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validatePassTransition_fromPending_fails() {
        val reQc = createSampleReQc(ReQcStatus.PENDING)
        val result = ReQcLifecycleValidator.validatePassTransition(reQc)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateFailTransition_missingReason_fails() {
        val reQc = createSampleReQc(ReQcStatus.IN_INSPECTION)
        val result = ReQcLifecycleValidator.validateFailTransition(reQc, null, "Defect not resolved")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateFailTransition_missingNotes_fails() {
        val reQc = createSampleReQc(ReQcStatus.IN_INSPECTION)
        val result = ReQcLifecycleValidator.validateFailTransition(reQc, ReQcFailureReason.DEFECT_REMAINS, "")
        assertTrue(result is DomainResult.Error)
    }
}

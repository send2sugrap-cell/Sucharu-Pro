package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ReQcAssignmentValidator] (Module 06 Step 06).
 */
class ReQcAssignmentTest {

    private fun createSampleReQc(status: ReQcStatus, inspectorId: String? = null): ReQcInspection {
        return ReQcInspection(
            reQcId = "reqc-001",
            productionJobId = "job-001",
            projectId = "proj-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            status = status,
            assignedInspectorId = inspectorId,
            createdBy = "user-001",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun validateAssignment_validParameters_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.PENDING)
        val result = ReQcAssignmentValidator.validateAssignment(
            reQc = reQc,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            callerRole = UserRole.MANAGER
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateAssignment_unauthorizedRole_fails() {
        val reQc = createSampleReQc(ReQcStatus.PENDING)
        val result = ReQcAssignmentValidator.validateAssignment(
            reQc = reQc,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            callerRole = UserRole.STAFF
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("not authorized to assign Re-QC"))
    }

    @Test
    fun validateAssignment_blankInspectorName_fails() {
        val reQc = createSampleReQc(ReQcStatus.PENDING)
        val result = ReQcAssignmentValidator.validateAssignment(
            reQc = reQc,
            inspectorId = "insp-01",
            inspectorName = "",
            callerRole = UserRole.ADMIN
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateAssignment_terminalReQc_fails() {
        val reQc = createSampleReQc(ReQcStatus.PASSED)
        val result = ReQcAssignmentValidator.validateAssignment(
            reQc = reQc,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            callerRole = UserRole.ADMIN
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateUnassignment_validAssignedReQc_isSuccess() {
        val reQc = createSampleReQc(ReQcStatus.ASSIGNED, inspectorId = "insp-01")
        val result = ReQcAssignmentValidator.validateUnassignment(reQc, callerRole = UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateUnassignment_noActiveAssignment_fails() {
        val reQc = createSampleReQc(ReQcStatus.PENDING, inspectorId = null)
        val result = ReQcAssignmentValidator.validateUnassignment(reQc, callerRole = UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
    }
}

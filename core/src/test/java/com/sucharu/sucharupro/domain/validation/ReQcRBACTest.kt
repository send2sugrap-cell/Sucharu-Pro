package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Role-Based Access Control in Re-QC subsystem (Module 06 Step 06).
 */
class ReQcRBACTest {

    private fun createSampleReQc(assignedInspectorId: String? = null): ReQcInspection {
        return ReQcInspection(
            reQcId = "reqc-001",
            productionJobId = "job-001",
            projectId = "proj-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            status = ReQcStatus.ASSIGNED,
            assignedInspectorId = assignedInspectorId,
            createdBy = "user-001",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun admin_hasFullMutationPermission() {
        val result = ReQcValidator.validateMutationPermission(UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun manager_hasFullMutationPermission() {
        val result = ReQcValidator.validateMutationPermission(UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun qcInspector_hasMutationPermission() {
        val result = ReQcValidator.validateMutationPermission(UserRole.QC_INSPECTOR)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun unauthorizedRoles_deniedMutation() {
        val unauthorized = listOf(
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )

        for (role in unauthorized) {
            val result = ReQcValidator.validateMutationPermission(role)
            assertTrue("Role $role should be denied", result is DomainResult.Error)
        }
    }

    @Test
    fun assignmentPermission_onlyAdminAndManagerAllowed() {
        assertTrue(ReQcAssignmentValidator.validateAssignmentPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(ReQcAssignmentValidator.validateAssignmentPermission(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(ReQcAssignmentValidator.validateAssignmentPermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
        assertTrue(ReQcAssignmentValidator.validateAssignmentPermission(UserRole.DESIGNER) is DomainResult.Error)
        assertTrue(ReQcAssignmentValidator.validateAssignmentPermission(UserRole.STAFF) is DomainResult.Error)
    }

    @Test
    fun executionPermission_assignedInspectorAllowed() {
        val reQc = createSampleReQc(assignedInspectorId = "insp-01")
        val result = ReQcAssignmentValidator.validateExecutionPermission(
            reQc = reQc,
            actorId = "insp-01",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun executionPermission_differentInspectorDenied() {
        val reQc = createSampleReQc(assignedInspectorId = "insp-01")
        val result = ReQcAssignmentValidator.validateExecutionPermission(
            reQc = reQc,
            actorId = "insp-99",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot execute Re-QC"))
    }
}

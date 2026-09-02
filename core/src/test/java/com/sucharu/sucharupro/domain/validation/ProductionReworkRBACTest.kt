package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Role-Based Access Control (RBAC) constraints for QC Rework (Module 06 Step 05).
 */
class ProductionReworkRBACTest {

    @Test
    fun mutationPermissions_authorizedRoles_succeed() {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)
        for (role in authorized) {
            val result = ProductionReworkValidator.validateMutationPermission(role)
            assertTrue("Role $role should be authorized for mutation", result is DomainResult.Success)
        }
    }

    @Test
    fun mutationPermissions_restrictedRoles_fail() {
        val restricted = listOf(
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )
        for (role in restricted) {
            val result = ProductionReworkValidator.validateMutationPermission(role)
            assertTrue("Role $role should be blocked from mutation", result is DomainResult.Error)
        }
    }

    @Test
    fun approvalPermissions_managementRoles_succeed() {
        val management = listOf(UserRole.ADMIN, UserRole.MANAGER)
        for (role in management) {
            val result = ProductionReworkValidator.validateApprovalPermission(role)
            assertTrue("Role $role should be authorized for approval", result is DomainResult.Success)
        }
    }

    @Test
    fun approvalPermissions_qcInspector_fails_dueToSeparationOfDuties() {
        val result = ProductionReworkValidator.validateApprovalPermission(UserRole.QC_INSPECTOR)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Requires Admin or Manager"))
    }

    @Test
    fun assignmentPermissions_onlyManagementAllowed() {
        assertTrue(ProductionReworkAssignmentValidator.validateAssignmentPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(ProductionReworkAssignmentValidator.validateAssignmentPermission(UserRole.MANAGER) is DomainResult.Success)

        val nonManagement = listOf(
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )
        for (role in nonManagement) {
            val result = ProductionReworkAssignmentValidator.validateAssignmentPermission(role)
            assertTrue("Role $role should be blocked from assigning rework", result is DomainResult.Error)
        }
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Role-Based Access Control (RBAC) tests for QC Defect Management (Module 06 Step 04).
 */
class ProductionDefectRBACTest {

    @Test
    fun defectManagementPermission_authorizedRoles_succeeds() {
        val authorized = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)
        for (role in authorized) {
            val res = ProductionDefectValidator.validateDefectPermission(role)
            assertTrue("Role $role should be authorized", res is DomainResult.Success)
        }
    }

    @Test
    fun defectManagementPermission_unauthorizedRoles_fails() {
        val unauthorized = listOf(
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )
        for (role in unauthorized) {
            val res = ProductionDefectValidator.validateDefectPermission(role)
            assertFalse("Role $role should be rejected", res.isSuccess)
        }
    }

    @Test
    fun closurePermission_restrictedToAdminAndManager() {
        assertTrue(ProductionDefectValidator.validateClosurePermission(UserRole.ADMIN).isSuccess)
        assertTrue(ProductionDefectValidator.validateClosurePermission(UserRole.MANAGER).isSuccess)

        // QC Inspector cannot formally close defects (requires admin/manager sign-off)
        assertFalse(ProductionDefectValidator.validateClosurePermission(UserRole.QC_INSPECTOR).isSuccess)
        assertFalse(ProductionDefectValidator.validateClosurePermission(UserRole.DESIGNER).isSuccess)
    }

    @Test
    fun assignmentPermission_authorizedRoles_succeeds() {
        assertTrue(ProductionDefectAssignmentValidator.validateAssignmentPermission(UserRole.ADMIN).isSuccess)
        assertTrue(ProductionDefectAssignmentValidator.validateAssignmentPermission(UserRole.MANAGER).isSuccess)
        assertTrue(ProductionDefectAssignmentValidator.validateAssignmentPermission(UserRole.QC_INSPECTOR).isSuccess)

        assertFalse(ProductionDefectAssignmentValidator.validateAssignmentPermission(UserRole.STAFF).isSuccess)
        assertFalse(ProductionDefectAssignmentValidator.validateAssignmentPermission(UserRole.CUSTOMER).isSuccess)
    }
}

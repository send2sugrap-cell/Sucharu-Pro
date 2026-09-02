package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC tests for [FinalQcAssignmentValidator] (Module 06 Step 07).
 */
class FinalQcRBACTest {

    @Test
    fun createPermission_allowedRoles() {
        assertTrue(FinalQcAssignmentValidator.validateCreatePermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(FinalQcAssignmentValidator.validateCreatePermission(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(FinalQcAssignmentValidator.validateCreatePermission(UserRole.QC_INSPECTOR) is DomainResult.Success)
    }

    @Test
    fun createPermission_disallowedRoles() {
        val disallowed = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )
        for (role in disallowed) {
            val result = FinalQcAssignmentValidator.validateCreatePermission(role)
            assertTrue("Role $role should be rejected for creation", result is DomainResult.Error)
        }
    }

    @Test
    fun assignmentPermission_onlyAdminAndManager() {
        assertTrue(FinalQcAssignmentValidator.validateAssignmentPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(FinalQcAssignmentValidator.validateAssignmentPermission(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(FinalQcAssignmentValidator.validateAssignmentPermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
        assertTrue(FinalQcAssignmentValidator.validateAssignmentPermission(UserRole.STAFF) is DomainResult.Error)
    }

    @Test
    fun cancelPermission_onlyAdminAndManager() {
        assertTrue(FinalQcAssignmentValidator.validateCancelPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(FinalQcAssignmentValidator.validateCancelPermission(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(FinalQcAssignmentValidator.validateCancelPermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
    }
}

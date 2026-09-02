package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC tests for QC operations (Module 06 Step 01).
 */
class ProductionQcRBACTest {

    @Test
    fun managementRoles_authorizedForCreationAndAssignment() {
        listOf(UserRole.ADMIN, UserRole.MANAGER).forEach { role ->
            assertTrue("Role ${role.name} should be allowed to manage QC", ProductionQcValidator.validateQcManagementPermission(role) is DomainResult.Success)
            assertTrue("Role ${role.name} should be allowed to assign inspectors", QcAssignmentValidator.validateAssignmentPermission(role) is DomainResult.Success)
        }
    }

    @Test
    fun qcInspector_authorizedForInspectionOnly() {
        assertTrue(QcAssignmentValidator.validateInspectionPermission(UserRole.QC_INSPECTOR) is DomainResult.Success)
        assertTrue(QcAssignmentValidator.validateAssignmentPermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
        assertTrue(ProductionQcValidator.validateQcManagementPermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
    }

    @Test
    fun restrictedRoles_deniedAllQcMutations() {
        val restricted = listOf(
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.WAREHOUSE,
            UserRole.ACCOUNTS
        )

        restricted.forEach { role ->
            assertTrue("Role ${role.name} should be denied management", ProductionQcValidator.validateQcManagementPermission(role) is DomainResult.Error)
            assertTrue("Role ${role.name} should be denied assignment", QcAssignmentValidator.validateAssignmentPermission(role) is DomainResult.Error)
            assertTrue("Role ${role.name} should be denied inspection execution", QcAssignmentValidator.validateInspectionPermission(role) is DomainResult.Error)
        }
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RBAC tests for Pre-Production QC inspection and submission (Module 06 Step 02).
 */
class PreProductionQcRBACTest {

    @Test
    fun authorizedRoles_canInspectAndSubmit() {
        listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR).forEach { role ->
            assertTrue("Role ${role.name} should be allowed to inspect Pre-Production QC", PreProductionQcValidator.validateInspectionPermission(role) is DomainResult.Success)
        }
    }

    @Test
    fun unauthorizedRoles_deniedPreProductionQcOperations() {
        listOf(
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        ).forEach { role ->
            assertTrue("Role ${role.name} should be denied Pre-Production QC operations", PreProductionQcValidator.validateInspectionPermission(role) is DomainResult.Error)
        }
    }
}

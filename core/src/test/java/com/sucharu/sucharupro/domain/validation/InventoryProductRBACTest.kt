package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryProductRBACTest {

    @Test
    fun `ADMIN and MANAGER have master admin permissions, others rejected`() {
        assertTrue(InventoryProductValidator.validateMasterAdminPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(InventoryProductValidator.validateMasterAdminPermission(UserRole.MANAGER) is DomainResult.Success)

        val rejectedRoles = listOf(
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS
        )
        for (role in rejectedRoles) {
            val res = InventoryProductValidator.validateMasterAdminPermission(role)
            assertTrue(res is DomainResult.Error)
        }
    }

    @Test
    fun `WAREHOUSE, STAFF, QC, DESIGNER and ACCOUNTS can view master records, external roles cannot`() {
        val allowedView = listOf(
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.ACCOUNTS
        )
        for (role in allowedView) {
            assertTrue(InventoryProductValidator.validateMasterViewPermission(role) is DomainResult.Success)
        }

        val deniedView = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        )
        for (role in deniedView) {
            assertTrue(InventoryProductValidator.validateMasterViewPermission(role) is DomainResult.Error)
        }
    }
}

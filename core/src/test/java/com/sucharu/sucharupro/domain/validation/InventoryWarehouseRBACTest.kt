package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryWarehouseRBACTest {

    @Test
    fun `ADMIN and MANAGER have warehouse admin permissions, others rejected`() {
        assertTrue(InventoryWarehouseValidator.validateWarehouseAdminPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(InventoryWarehouseValidator.validateWarehouseAdminPermission(UserRole.MANAGER) is DomainResult.Success)

        val rejected = listOf(
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS
        )
        for (role in rejected) {
            assertTrue(InventoryWarehouseValidator.validateWarehouseAdminPermission(role) is DomainResult.Error)
        }
    }

    @Test
    fun `internal roles can view warehouses, external roles cannot`() {
        val allowed = listOf(
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.ACCOUNTS
        )
        for (role in allowed) {
            assertTrue(InventoryWarehouseValidator.validateWarehouseViewPermission(role) is DomainResult.Success)
        }

        val denied = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        )
        for (role in denied) {
            assertTrue(InventoryWarehouseValidator.validateWarehouseViewPermission(role) is DomainResult.Error)
        }
    }
}

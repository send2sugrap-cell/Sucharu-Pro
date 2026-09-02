package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationRBACTest {

    @Test
    fun `ADMIN and MANAGER have location management permissions, others rejected`() {
        assertTrue(InventoryLocationValidator.validateLocationAdminPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(InventoryLocationValidator.validateLocationAdminPermission(UserRole.MANAGER) is DomainResult.Success)

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
            assertTrue(InventoryLocationValidator.validateLocationAdminPermission(role) is DomainResult.Error)
        }
    }

    @Test
    fun `internal roles can view locations, external roles cannot`() {
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
            assertTrue(InventoryLocationValidator.validateLocationViewPermission(role) is DomainResult.Success)
        }

        val denied = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        )
        for (role in denied) {
            assertTrue(InventoryLocationValidator.validateLocationViewPermission(role) is DomainResult.Error)
        }
    }
}

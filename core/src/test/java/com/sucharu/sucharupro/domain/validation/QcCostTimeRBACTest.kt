package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class QcCostTimeRBACTest {

    @Test
    fun `ADMIN and MANAGER are authorized for all operations`() {
        listOf(UserRole.ADMIN, UserRole.MANAGER).forEach { role ->
            assertTrue(QcCostTimeReconciliationValidator.validateRecordPermission(role) is DomainResult.Success)
            assertTrue(QcCostTimeReconciliationValidator.validateReconcilePermission(role) is DomainResult.Success)
            assertTrue(QcCostTimeReconciliationValidator.validateAdjustmentPermission(role) is DomainResult.Success)
            assertTrue(QcCostTimeReconciliationValidator.validateLockPermission(role) is DomainResult.Success)
        }
    }

    @Test
    fun `QC_INSPECTOR can record and reconcile but cannot adjust or lock`() {
        val role = UserRole.QC_INSPECTOR
        assertTrue(QcCostTimeReconciliationValidator.validateRecordPermission(role) is DomainResult.Success)
        assertTrue(QcCostTimeReconciliationValidator.validateReconcilePermission(role) is DomainResult.Success)
        assertTrue(QcCostTimeReconciliationValidator.validateAdjustmentPermission(role) is DomainResult.Error)
        assertTrue(QcCostTimeReconciliationValidator.validateLockPermission(role) is DomainResult.Error)
    }

    @Test
    fun `Restricted roles are denied all operations`() {
        val restricted = listOf(
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )
        restricted.forEach { role ->
            assertTrue(QcCostTimeReconciliationValidator.validateRecordPermission(role) is DomainResult.Error)
            assertTrue(QcCostTimeReconciliationValidator.validateReconcilePermission(role) is DomainResult.Error)
            assertTrue(QcCostTimeReconciliationValidator.validateAdjustmentPermission(role) is DomainResult.Error)
            assertTrue(QcCostTimeReconciliationValidator.validateLockPermission(role) is DomainResult.Error)
        }
    }
}

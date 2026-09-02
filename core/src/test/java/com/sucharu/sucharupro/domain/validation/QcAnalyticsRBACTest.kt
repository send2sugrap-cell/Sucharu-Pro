package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class QcAnalyticsRBACTest {

    @Test
    fun `ADMIN, MANAGER and QC_INSPECTOR are authorized`() {
        assertTrue(QcAnalyticsValidator.validateRbac(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(QcAnalyticsValidator.validateRbac(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(QcAnalyticsValidator.validateRbac(UserRole.QC_INSPECTOR) is DomainResult.Success)
    }

    @Test
    fun `unauthorized roles are rejected`() {
        val unauthorizedRoles = listOf(
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )

        unauthorizedRoles.forEach { role ->
            val result = QcAnalyticsValidator.validateRbac(role)
            assertTrue("Role $role should be rejected", result is DomainResult.Error)
            assertTrue((result as DomainResult.Error).message.contains("is not authorized to access QC analytics"))
        }
    }
}

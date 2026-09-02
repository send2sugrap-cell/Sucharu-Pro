package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class QcGovernanceRBACTest {

    @Test
    fun `ADMIN and MANAGER have config permission, other roles rejected`() {
        assertTrue(QcGovernanceValidator.validateConfigPermission(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateConfigPermission(UserRole.MANAGER) is DomainResult.Success)

        val rejectedRoles = listOf(
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.ACCOUNTS,
            UserRole.WAREHOUSE
        )
        for (role in rejectedRoles) {
            val result = QcGovernanceValidator.validateConfigPermission(role)
            assertTrue(result is DomainResult.Error)
            assertTrue((result as DomainResult.Error).message.contains("not authorized", ignoreCase = true))
        }
    }

    @Test
    fun `QC_INSPECTOR can propose actions but cannot approve them`() {
        assertTrue(QcGovernanceValidator.validateActionProposePermission(UserRole.QC_INSPECTOR) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionApprovePermission(UserRole.QC_INSPECTOR) is DomainResult.Error)
    }
}

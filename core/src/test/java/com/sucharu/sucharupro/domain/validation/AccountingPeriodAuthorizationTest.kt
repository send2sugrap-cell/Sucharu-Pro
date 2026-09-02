package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountingPeriodAuthorizationTest {

    @Test
    fun `validateCreateReconciliation allows Admin, Manager, Accounts and rejects others`() {
        assertTrue(FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(UserRole.ACCOUNTS) is DomainResult.Success)

        assertTrue(FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(UserRole.STAFF) is DomainResult.Error)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(UserRole.CUSTOMER) is DomainResult.Error)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateCreateReconciliation(UserRole.VENDOR) is DomainResult.Error)
    }

    @Test
    fun `validateClosePeriod enforces separation of duties for non-admin`() {
        // Same initiator and closer blocked for non-admin
        val nonAdminSelfClose = FinancialReconciliationAuthorizationValidator.validateClosePeriod(
            callerRole = UserRole.ACCOUNTS,
            initiatorId = "USER_1",
            closerId = "USER_1"
        )
        assertTrue(nonAdminSelfClose is DomainResult.Error)

        // Different initiator and closer allowed for ACCOUNTS
        val nonAdminSeparateClose = FinancialReconciliationAuthorizationValidator.validateClosePeriod(
            callerRole = UserRole.ACCOUNTS,
            initiatorId = "USER_1",
            closerId = "USER_2"
        )
        assertTrue(nonAdminSeparateClose is DomainResult.Success)

        // ADMIN can self-close if necessary
        val adminSelfClose = FinancialReconciliationAuthorizationValidator.validateClosePeriod(
            callerRole = UserRole.ADMIN,
            initiatorId = "ADMIN_1",
            closerId = "ADMIN_1"
        )
        assertTrue(adminSelfClose is DomainResult.Success)
    }

    @Test
    fun `validateReopenPeriod and validateWaiveCriticalDiscrepancy are restricted to ADMIN`() {
        assertTrue(FinancialReconciliationAuthorizationValidator.validateReopenPeriod(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateReopenPeriod(UserRole.MANAGER) is DomainResult.Error)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateReopenPeriod(UserRole.ACCOUNTS) is DomainResult.Error)

        assertTrue(FinancialReconciliationAuthorizationValidator.validateWaiveCriticalDiscrepancy(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateWaiveCriticalDiscrepancy(UserRole.MANAGER) is DomainResult.Error)
        assertTrue(FinancialReconciliationAuthorizationValidator.validateWaiveCriticalDiscrepancy(UserRole.STAFF) is DomainResult.Error)
    }
}

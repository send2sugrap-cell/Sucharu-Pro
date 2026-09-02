package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.FinancialAuthorizationValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialTransactionAuthorizationTest {

    @Test
    fun `internal roles have transaction view access while external roles are rejected`() {
        assertTrue(FinancialAuthorizationValidator.validateViewTransactions(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(FinancialAuthorizationValidator.validateViewTransactions(UserRole.ACCOUNTS) is DomainResult.Success)
        assertTrue(FinancialAuthorizationValidator.validateViewTransactions(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(FinancialAuthorizationValidator.validateViewTransactions(UserRole.STAFF) is DomainResult.Success)

        assertTrue(FinancialAuthorizationValidator.validateViewTransactions(UserRole.CUSTOMER) is DomainResult.Error)
        assertTrue(FinancialAuthorizationValidator.validateViewTransactions(UserRole.VENDOR) is DomainResult.Error)
    }

    @Test
    fun `separation of duties enforces non-admin creator cannot post own transaction`() {
        // Accounts user posting own transaction -> ERROR
        val accountsPostingOwn = FinancialAuthorizationValidator.validatePostTransaction(
            callerRole = UserRole.ACCOUNTS,
            creatorId = "user-accounts-1",
            actorId = "user-accounts-1"
        )
        assertTrue(accountsPostingOwn is DomainResult.Error)

        // Accounts user posting someone else's transaction -> SUCCESS
        val accountsPostingOther = FinancialAuthorizationValidator.validatePostTransaction(
            callerRole = UserRole.ACCOUNTS,
            creatorId = "user-staff-1",
            actorId = "user-accounts-1"
        )
        assertTrue(accountsPostingOther is DomainResult.Success)

        // Admin posting own transaction -> ALLOWED
        val adminPostingOwn = FinancialAuthorizationValidator.validatePostTransaction(
            callerRole = UserRole.ADMIN,
            creatorId = "admin-1",
            actorId = "admin-1"
        )
        assertTrue(adminPostingOwn is DomainResult.Success)
    }
}

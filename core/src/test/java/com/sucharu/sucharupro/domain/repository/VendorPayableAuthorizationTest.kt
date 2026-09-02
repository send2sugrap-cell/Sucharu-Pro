package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.VendorPayableAuthorizationValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorPayableAuthorizationTest {

    @Test
    fun `internal roles can create and manage payables`() {
        assertTrue(VendorPayableAuthorizationValidator.validateCreatePayable(UserRole.STAFF) is DomainResult.Success)
        assertTrue(VendorPayableAuthorizationValidator.validateCreatePayable(UserRole.ACCOUNTS) is DomainResult.Success)
        assertTrue(VendorPayableAuthorizationValidator.validateCreatePayable(UserRole.ADMIN) is DomainResult.Success)

        assertTrue(VendorPayableAuthorizationValidator.validateCreatePayable(UserRole.CUSTOMER) is DomainResult.Error)
        assertTrue(VendorPayableAuthorizationValidator.validateCreatePayable(UserRole.VENDOR) is DomainResult.Error)
    }

    @Test
    fun `separation of duties prohibits creator from approving own payable unless admin`() {
        // Accounts officer cannot approve own created payable
        val nonAdminSelfApprove = VendorPayableAuthorizationValidator.validateApprovePayable(
            callerRole = UserRole.ACCOUNTS,
            creatorId = "acct-1",
            approverId = "acct-1"
        )
        assertTrue(nonAdminSelfApprove is DomainResult.Error)

        // Independent Accounts officer can approve
        val independentApprove = VendorPayableAuthorizationValidator.validateApprovePayable(
            callerRole = UserRole.ACCOUNTS,
            creatorId = "staff-1",
            approverId = "acct-1"
        )
        assertTrue(independentApprove is DomainResult.Success)

        // Admin can approve own created payable
        val adminSelfApprove = VendorPayableAuthorizationValidator.validateApprovePayable(
            callerRole = UserRole.ADMIN,
            creatorId = "admin-1",
            approverId = "admin-1"
        )
        assertTrue(adminSelfApprove is DomainResult.Success)
    }
}

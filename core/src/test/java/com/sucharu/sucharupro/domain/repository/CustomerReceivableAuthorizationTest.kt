package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.CustomerReceivableAuthorizationValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerReceivableAuthorizationTest {

    @Test
    fun `internal financial roles are authorized for creation and update`() {
        assertTrue(CustomerReceivableAuthorizationValidator.validateCreateReceivable(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(CustomerReceivableAuthorizationValidator.validateCreateReceivable(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(CustomerReceivableAuthorizationValidator.validateCreateReceivable(UserRole.ACCOUNTS) is DomainResult.Success)

        assertTrue(CustomerReceivableAuthorizationValidator.validateUpdateReceivable(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(CustomerReceivableAuthorizationValidator.validateUpdateReceivable(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(CustomerReceivableAuthorizationValidator.validateUpdateReceivable(UserRole.ACCOUNTS) is DomainResult.Success)
    }

    @Test
    fun `staff and external roles cannot create receivables`() {
        assertTrue(CustomerReceivableAuthorizationValidator.validateCreateReceivable(UserRole.STAFF) is DomainResult.Error)
        assertTrue(CustomerReceivableAuthorizationValidator.validateCreateReceivable(UserRole.CUSTOMER) is DomainResult.Error)
        assertTrue(CustomerReceivableAuthorizationValidator.validateCreateReceivable(UserRole.VENDOR) is DomainResult.Error)
    }

    @Test
    fun `customers can only view their own scoped receivables`() {
        // Own customer view -> Allowed
        assertTrue(
            CustomerReceivableAuthorizationValidator.validateViewReceivables(
                callerRole = UserRole.CUSTOMER,
                requestedCustomerId = "CUST-001",
                authenticatedCustomerId = "CUST-001"
            ) is DomainResult.Success
        )

        // Other customer view -> Rejected
        assertTrue(
            CustomerReceivableAuthorizationValidator.validateViewReceivables(
                callerRole = UserRole.CUSTOMER,
                requestedCustomerId = "CUST-002",
                authenticatedCustomerId = "CUST-001"
            ) is DomainResult.Error
        )
    }
}

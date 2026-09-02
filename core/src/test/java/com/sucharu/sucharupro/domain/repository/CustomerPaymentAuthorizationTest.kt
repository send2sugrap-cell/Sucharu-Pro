package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.CustomerPaymentAuthorizationValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerPaymentAuthorizationTest {

    @Test
    fun `internal roles can create and update payments`() {
        assertTrue(CustomerPaymentAuthorizationValidator.validateCreateDraftPayment(UserRole.STAFF) is DomainResult.Success)
        assertTrue(CustomerPaymentAuthorizationValidator.validateCreateDraftPayment(UserRole.ACCOUNTS) is DomainResult.Success)
        assertTrue(CustomerPaymentAuthorizationValidator.validateCreateDraftPayment(UserRole.ADMIN) is DomainResult.Success)

        assertTrue(CustomerPaymentAuthorizationValidator.validateCreateDraftPayment(UserRole.CUSTOMER) is DomainResult.Error)
        assertTrue(CustomerPaymentAuthorizationValidator.validateCreateDraftPayment(UserRole.VENDOR) is DomainResult.Error)
    }

    @Test
    fun `separation of duties prohibits creator from posting own payment unless admin`() {
        // Accounts officer cannot approve/post their own created payment
        val nonAdminSelfPost = CustomerPaymentAuthorizationValidator.validatePostPayment(
            callerRole = UserRole.ACCOUNTS,
            creatorId = "acct-1",
            posterId = "acct-1"
        )
        assertTrue(nonAdminSelfPost is DomainResult.Error)

        // Independent Accounts officer can approve/post
        val independentPost = CustomerPaymentAuthorizationValidator.validatePostPayment(
            callerRole = UserRole.ACCOUNTS,
            creatorId = "staff-1",
            posterId = "acct-1"
        )
        assertTrue(independentPost is DomainResult.Success)

        // Admin can approve own payment
        val adminSelfPost = CustomerPaymentAuthorizationValidator.validatePostPayment(
            callerRole = UserRole.ADMIN,
            creatorId = "admin-1",
            posterId = "admin-1"
        )
        assertTrue(adminSelfPost is DomainResult.Success)
    }

    @Test
    fun `customers can only observe their own payments`() {
        assertTrue(
            CustomerPaymentAuthorizationValidator.validateViewPayments(
                callerRole = UserRole.CUSTOMER,
                requestedCustomerId = "CUST-001",
                authenticatedCustomerId = "CUST-001"
            ) is DomainResult.Success
        )

        assertTrue(
            CustomerPaymentAuthorizationValidator.validateViewPayments(
                callerRole = UserRole.CUSTOMER,
                requestedCustomerId = "CUST-002",
                authenticatedCustomerId = "CUST-001"
            ) is DomainResult.Error
        )
    }
}

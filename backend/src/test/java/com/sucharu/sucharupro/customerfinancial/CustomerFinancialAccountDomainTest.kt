package com.sucharu.sucharupro.customerfinancial

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.validation.customerfinancial.CustomerFinancialAccountValidator
import org.junit.Assert.*
import org.junit.Test

/**
 * MODULE 14 STEP 01: Domain Model & Validator Unit Tests.
 */
class CustomerFinancialAccountDomainTest {

    @Test
    fun testStatusTransitions() {
        // ACTIVE -> SUSPENDED (Allowed)
        assertTrue(CustomerFinancialAccountStatus.ACTIVE.canTransitionTo(CustomerFinancialAccountStatus.SUSPENDED))

        // ACTIVE -> CLOSED (Allowed)
        assertTrue(CustomerFinancialAccountStatus.ACTIVE.canTransitionTo(CustomerFinancialAccountStatus.CLOSED))

        // SUSPENDED -> ACTIVE (Allowed)
        assertTrue(CustomerFinancialAccountStatus.SUSPENDED.canTransitionTo(CustomerFinancialAccountStatus.ACTIVE))

        // SUSPENDED -> CLOSED (Allowed)
        assertTrue(CustomerFinancialAccountStatus.SUSPENDED.canTransitionTo(CustomerFinancialAccountStatus.CLOSED))

        // CLOSED is terminal
        assertFalse(CustomerFinancialAccountStatus.CLOSED.canTransitionTo(CustomerFinancialAccountStatus.ACTIVE))
        assertFalse(CustomerFinancialAccountStatus.CLOSED.canTransitionTo(CustomerFinancialAccountStatus.SUSPENDED))
    }

    @Test
    fun testCreationValidation() {
        // Valid
        val valid = CustomerFinancialAccountValidator.validateCreation(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            customerId = "CUS-001",
            currency = "BDT"
        )
        assertTrue(valid is DomainResult.Success)

        // Blank customer
        val invalidCust = CustomerFinancialAccountValidator.validateCreation(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            customerId = "",
            currency = "BDT"
        )
        assertTrue(invalidCust is DomainResult.Error)

        // Invalid currency
        val invalidCurr = CustomerFinancialAccountValidator.validateCreation(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            customerId = "CUS-001",
            currency = "INVALID"
        )
        assertTrue(invalidCurr is DomainResult.Error)
    }

    @Test
    fun testStatusTransitionValidation() {
        val account = CustomerFinancialAccount(
            financialAccountId = "CFA-001",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            customerId = "CUS-001",
            accountNumber = "ACC-CUS-001",
            status = CustomerFinancialAccountStatus.ACTIVE
        )

        // Suspend without reason -> error
        val suspendNoReason = CustomerFinancialAccountValidator.validateStatusTransition(
            account,
            CustomerFinancialAccountStatus.SUSPENDED,
            null
        )
        assertTrue(suspendNoReason is DomainResult.Error)

        // Suspend with reason -> success
        val suspendWithReason = CustomerFinancialAccountValidator.validateStatusTransition(
            account,
            CustomerFinancialAccountStatus.SUSPENDED,
            "Temporarily suspended for credit review"
        )
        assertTrue(suspendWithReason is DomainResult.Success)

        // Closed account transition -> error
        val closedAccount = account.copy(status = CustomerFinancialAccountStatus.CLOSED)
        val transitionFromClosed = CustomerFinancialAccountValidator.validateStatusTransition(
            closedAccount,
            CustomerFinancialAccountStatus.ACTIVE,
            "Reopening"
        )
        assertTrue(transitionFromClosed is DomainResult.Error)
    }
}

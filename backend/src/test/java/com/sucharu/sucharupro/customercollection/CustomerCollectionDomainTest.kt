package com.sucharu.sucharupro.customercollection

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customercollection.*
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.validation.customercollection.CustomerCollectionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerCollectionDomainTest {

    private val validCustomer = Customer(
        customerId = "CUS-001",
        customerCode = "CUS-001",
        displayName = "Valid Customer",
        primaryPhone = "+8801700000001",
        customerType = CustomerType.BUSINESS,
        status = CustomerStatusType.ACTIVE,
        createdAt = "2026-08-29T00:00:00Z",
        updatedAt = "2026-08-29T00:00:00Z"
    )

    private val validInvoice = CustomerInvoice(
        invoiceId = "INV-001",
        tenantId = "TENANT-01",
        projectId = "PRJ-01",
        customerId = "CUS-001",
        customerFinancialAccountId = "CFA-001",
        invoiceNumber = "INV-001",
        grandTotal = BigDecimal("10000.0000"),
        dueAmount = BigDecimal("10000.0000"),
        status = CustomerInvoiceStatus.ISSUED
    )

    @Test
    fun testValidCreateActionValidation() {
        val result = CustomerCollectionValidator.validateCreateAction(
            customer = validCustomer,
            invoice = validInvoice,
            scheduledAt = System.currentTimeMillis() + 86400000,
            assignedUserId = "staff_01",
            actorId = "admin_01"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun testCreateActionWithSuspendedCustomerRejected() {
        val blocked = validCustomer.copy(status = CustomerStatusType.BLOCKED)
        val result = CustomerCollectionValidator.validateCreateAction(
            customer = blocked,
            invoice = null,
            scheduledAt = System.currentTimeMillis(),
            assignedUserId = null,
            actorId = "admin_01"
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testInvoiceOwnershipValidation() {
        val otherCustInvoice = validInvoice.copy(customerId = "OTHER-CUS")
        val result = CustomerCollectionValidator.validateInvoice(otherCustInvoice, "CUS-001")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testRescheduleCompletedActionRejected() {
        val action = CustomerCollectionAction(
            actionId = "ACT-01",
            tenantId = "TENANT-01",
            projectId = "PRJ-01",
            customerId = "CUS-001",
            status = CollectionActionStatus.COMPLETED,
            scheduledAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            createdBy = "staff_01",
            updatedAt = System.currentTimeMillis(),
            updatedBy = "staff_01"
        )
        val result = CustomerCollectionValidator.validateRescheduleAction(action, System.currentTimeMillis() + 5000, "staff_01")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testCompleteActionRequiresOutcome() {
        val action = CustomerCollectionAction(
            actionId = "ACT-01",
            tenantId = "TENANT-01",
            projectId = "PRJ-01",
            customerId = "CUS-001",
            status = CollectionActionStatus.SCHEDULED,
            scheduledAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            createdBy = "staff_01",
            updatedAt = System.currentTimeMillis(),
            updatedBy = "staff_01"
        )
        val result = CustomerCollectionValidator.validateCompleteAction(action, null, "staff_01")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testCancelActionRequiresReason() {
        val action = CustomerCollectionAction(
            actionId = "ACT-01",
            tenantId = "TENANT-01",
            projectId = "PRJ-01",
            customerId = "CUS-001",
            status = CollectionActionStatus.SCHEDULED,
            scheduledAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            createdBy = "staff_01",
            updatedAt = System.currentTimeMillis(),
            updatedBy = "staff_01"
        )
        val result = CustomerCollectionValidator.validateCancelAction(action, "", "staff_01")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun testPaymentPromiseValidation() {
        // Exceeding outstanding should be rejected
        val result = CustomerCollectionValidator.validatePaymentPromise(
            customer = validCustomer,
            invoice = validInvoice,
            promisedAmount = BigDecimal("20000.0000"), // exceeds 10,000
            promisedDate = System.currentTimeMillis() + 86400000,
            totalOutstanding = BigDecimal("10000.0000"),
            actorId = "staff_01"
        )
        assertTrue(result is DomainResult.Error)

        // Valid promise
        val validPromiseRes = CustomerCollectionValidator.validatePaymentPromise(
            customer = validCustomer,
            invoice = validInvoice,
            promisedAmount = BigDecimal("5000.0000"),
            promisedDate = System.currentTimeMillis() + 86400000,
            totalOutstanding = BigDecimal("10000.0000"),
            actorId = "staff_01"
        )
        assertTrue(validPromiseRes is DomainResult.Success)
    }
}

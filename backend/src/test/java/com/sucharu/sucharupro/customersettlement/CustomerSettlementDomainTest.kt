package com.sucharu.sucharupro.customersettlement

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.InvoiceAllocationRequestItem
import com.sucharu.sucharupro.domain.validation.customersettlement.CustomerSettlementValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerSettlementDomainTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"
    private val accountId = "CFA-001"

    private val account = CustomerFinancialAccount(
        financialAccountId = accountId,
        tenantId = tenantId,
        projectId = projectId,
        customerId = customerId,
        accountNumber = "CFA-10001",
        status = CustomerFinancialAccountStatus.ACTIVE
    )

    private val confirmedPayment = CustomerPayment(
        paymentId = "PAY-001",
        tenantId = tenantId,
        projectId = projectId,
        paymentNumber = "PAY-1001",
        customerId = customerId,
        customerFinancialAccountId = accountId,
        amount = BigDecimal("10000.0000"),
        status = CustomerPaymentStatus.CONFIRMED
    )

    private val issuedInvoice = CustomerInvoice(
        invoiceId = "INV-001",
        tenantId = tenantId,
        projectId = projectId,
        customerId = customerId,
        customerFinancialAccountId = accountId,
        invoiceNumber = "INV-1001",
        grandTotal = BigDecimal("8000.0000"),
        dueAmount = BigDecimal("8000.0000"),
        status = CustomerInvoiceStatus.ISSUED
    )

    @Test
    fun testValidAllocation() {
        val res = CustomerSettlementValidator.validateAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = confirmedPayment,
            invoice = issuedInvoice,
            account = account,
            amount = BigDecimal("5000.0000"),
            currentPaymentAllocatedAmount = BigDecimal.ZERO
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testRejectOverAllocationAgainstPayment() {
        val res = CustomerSettlementValidator.validateAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = confirmedPayment,
            invoice = issuedInvoice,
            account = account,
            amount = BigDecimal("11000.0000"),
            currentPaymentAllocatedAmount = BigDecimal.ZERO
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testRejectOverAllocationAgainstInvoiceDue() {
        val res = CustomerSettlementValidator.validateAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = confirmedPayment,
            invoice = issuedInvoice,
            account = account,
            amount = BigDecimal("9000.0000"),
            currentPaymentAllocatedAmount = BigDecimal.ZERO
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testRejectUnconfirmedPaymentAllocation() {
        val recordedPayment = confirmedPayment.copy(status = CustomerPaymentStatus.RECORDED)
        val res = CustomerSettlementValidator.validateAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = recordedPayment,
            invoice = issuedInvoice,
            account = account,
            amount = BigDecimal("5000.0000"),
            currentPaymentAllocatedAmount = BigDecimal.ZERO
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testMultiAllocationValidation() {
        val items = listOf(
            InvoiceAllocationRequestItem("INV-001", BigDecimal("5000.0000")),
            InvoiceAllocationRequestItem("INV-002", BigDecimal("3000.0000"))
        )
        val res = CustomerSettlementValidator.validateMultiAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = confirmedPayment,
            account = account,
            items = items,
            currentPaymentAllocatedAmount = BigDecimal.ZERO
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testRejectMultiAllocationDuplicateInvoices() {
        val items = listOf(
            InvoiceAllocationRequestItem("INV-001", BigDecimal("2000.0000")),
            InvoiceAllocationRequestItem("INV-001", BigDecimal("3000.0000"))
        )
        val res = CustomerSettlementValidator.validateMultiAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = confirmedPayment,
            account = account,
            items = items,
            currentPaymentAllocatedAmount = BigDecimal.ZERO
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testReversalValidation() {
        val allocation = CustomerPaymentAllocation(
            allocationId = "ALC-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            paymentId = "PAY-001",
            invoiceId = "INV-001",
            allocatedAmount = BigDecimal("5000.0000"),
            status = CustomerPaymentAllocationStatus.ALLOCATED
        )

        val validReversal = CustomerSettlementValidator.validateReversal(allocation, "Accounting adjustment")
        assertTrue(validReversal is DomainResult.Success)

        val invalidNoReason = CustomerSettlementValidator.validateReversal(allocation, "")
        assertTrue(invalidNoReason is DomainResult.Error)

        val alreadyReversed = allocation.copy(status = CustomerPaymentAllocationStatus.REVERSED)
        val invalidAlreadyReversed = CustomerSettlementValidator.validateReversal(alreadyReversed, "Reason")
        assertTrue(invalidAlreadyReversed is DomainResult.Error)
    }
}

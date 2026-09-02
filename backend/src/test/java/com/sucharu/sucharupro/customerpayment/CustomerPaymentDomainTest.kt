package com.sucharu.sucharupro.customerpayment

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.validation.customerpayment.CustomerPaymentValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 03: Customer Payment Domain & Validator Tests.
 */
class CustomerPaymentDomainTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"
    private val accountId = "CFA-001"

    private val activeAccount = CustomerFinancialAccount(
        financialAccountId = accountId,
        tenantId = tenantId,
        projectId = projectId,
        customerId = customerId,
        accountNumber = "ACC-001",
        status = CustomerFinancialAccountStatus.ACTIVE
    )

    private val issuedInvoice = CustomerInvoice(
        invoiceId = "INV-001",
        tenantId = tenantId,
        projectId = projectId,
        customerId = customerId,
        customerFinancialAccountId = accountId,
        invoiceNumber = "INV-2026-001",
        grandTotal = BigDecimal("5000.0000"),
        paidAmount = BigDecimal.ZERO,
        dueAmount = BigDecimal("5000.0000"),
        status = CustomerInvoiceStatus.ISSUED
    )

    @Test
    fun testValidDirectInvoicePayment() {
        val res = CustomerPaymentValidator.validatePaymentRecording(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            financialAccountId = accountId,
            amount = BigDecimal("2000.0000"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BKASH,
            referenceNumber = "TRX-123456",
            account = activeAccount,
            invoice = issuedInvoice
        )
        assertTrue("Valid invoice payment must pass", res is DomainResult.Success)
    }

    @Test
    fun testZeroOrNegativeAmountRejected() {
        val zeroRes = CustomerPaymentValidator.validatePaymentRecording(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            financialAccountId = accountId,
            amount = BigDecimal.ZERO,
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            referenceNumber = null,
            account = activeAccount,
            invoice = issuedInvoice
        )
        assertTrue("Zero amount must be rejected", zeroRes is DomainResult.Error)

        val negRes = CustomerPaymentValidator.validatePaymentRecording(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            financialAccountId = accountId,
            amount = BigDecimal("-500.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            referenceNumber = null,
            account = activeAccount,
            invoice = issuedInvoice
        )
        assertTrue("Negative amount must be rejected", negRes is DomainResult.Error)
    }

    @Test
    fun testOverpaymentExceedingInvoiceDueRejected() {
        val res = CustomerPaymentValidator.validatePaymentRecording(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            financialAccountId = accountId,
            amount = BigDecimal("5000.01"), // Exceeds 5000.00 due
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK,
            referenceNumber = "CHQ-999",
            account = activeAccount,
            invoice = issuedInvoice
        )
        assertTrue("Overpayment must be rejected", res is DomainResult.Error)
    }

    @Test
    fun testPaymentAgainstCancelledOrVoidInvoiceRejected() {
        val cancelledInvoice = issuedInvoice.copy(status = CustomerInvoiceStatus.CANCELLED)
        val res = CustomerPaymentValidator.validatePaymentRecording(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            financialAccountId = accountId,
            amount = BigDecimal("1000.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            referenceNumber = null,
            account = activeAccount,
            invoice = cancelledInvoice
        )
        assertTrue("Payment on cancelled invoice must be rejected", res is DomainResult.Error)
    }

    @Test
    fun testPaymentStatusTransitions() {
        // RECORDED -> CONFIRMED (Valid)
        assertTrue(CustomerPaymentStatus.RECORDED.canTransitionTo(CustomerPaymentStatus.CONFIRMED))
        // RECORDED -> CANCELLED (Valid)
        assertTrue(CustomerPaymentStatus.RECORDED.canTransitionTo(CustomerPaymentStatus.CANCELLED))
        // CONFIRMED -> CANCELLED (Valid)
        assertTrue(CustomerPaymentStatus.CONFIRMED.canTransitionTo(CustomerPaymentStatus.CANCELLED))
        // CANCELLED -> CONFIRMED (Invalid - terminal)
        assertFalse(CustomerPaymentStatus.CANCELLED.canTransitionTo(CustomerPaymentStatus.CONFIRMED))
    }

    @Test
    fun testPaymentCancellationRequiresReason() {
        val payment = CustomerPayment(
            paymentId = "PAY-001",
            tenantId = tenantId,
            projectId = projectId,
            paymentNumber = "PAY-2026-001",
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("1000.00")
        )

        val noReasonRes = CustomerPaymentValidator.validateStatusTransition(
            payment, CustomerPaymentStatus.CANCELLED, reason = null
        )
        assertTrue("Cancellation without reason must fail", noReasonRes is DomainResult.Error)

        val withReasonRes = CustomerPaymentValidator.validateStatusTransition(
            payment, CustomerPaymentStatus.CANCELLED, reason = "Bounced cheque"
        )
        assertTrue("Cancellation with reason must succeed", withReasonRes is DomainResult.Success)
    }
}

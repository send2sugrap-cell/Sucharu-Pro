package com.sucharu.sucharupro.customercredit

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.validation.customercredit.CustomerCreditValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditDomainTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUST-001"
    private val accountId = "CFA-001"

    private val validAccount = CustomerFinancialAccount(
        financialAccountId = accountId,
        tenantId = tenantId,
        projectId = projectId,
        customerId = customerId,
        accountNumber = "CFA-10001",
        status = CustomerFinancialAccountStatus.ACTIVE
    )

    private val validInvoice = CustomerInvoice(
        invoiceId = "INV-001",
        tenantId = tenantId,
        projectId = projectId,
        customerId = customerId,
        customerFinancialAccountId = accountId,
        invoiceNumber = "INV-2026-0001",
        subtotal = BigDecimal("1000.0000"),
        grandTotal = BigDecimal("1150.0000"),
        paidAmount = BigDecimal("0.0000"),
        dueAmount = BigDecimal("1150.0000"),
        status = CustomerInvoiceStatus.ISSUED
    )

    @Test
    fun testAdvanceStatusLifecycle() {
        val status = CustomerAdvanceStatus.RECORDED
        assertTrue(status.canTransitionTo(CustomerAdvanceStatus.AVAILABLE))
        assertTrue(status.canTransitionTo(CustomerAdvanceStatus.ALLOCATED))
        assertTrue(status.canTransitionTo(CustomerAdvanceStatus.EXHAUSTED))
        assertTrue(status.canTransitionTo(CustomerAdvanceStatus.CANCELLED))

        val exhausted = CustomerAdvanceStatus.EXHAUSTED
        assertTrue(exhausted.canTransitionTo(CustomerAdvanceStatus.AVAILABLE))
        assertFalse(exhausted.canTransitionTo(CustomerAdvanceStatus.CANCELLED))

        val cancelled = CustomerAdvanceStatus.CANCELLED
        assertFalse(cancelled.canTransitionTo(CustomerAdvanceStatus.AVAILABLE))
    }

    @Test
    fun testRefundStatusLifecycle() {
        val status = CustomerRefundStatus.REQUESTED
        assertTrue(status.canTransitionTo(CustomerRefundStatus.APPROVED))
        assertTrue(status.canTransitionTo(CustomerRefundStatus.REJECTED))
        assertTrue(status.canTransitionTo(CustomerRefundStatus.CANCELLED))

        val approved = CustomerRefundStatus.APPROVED
        assertTrue(approved.canTransitionTo(CustomerRefundStatus.PROCESSED))
        assertTrue(approved.canTransitionTo(CustomerRefundStatus.COMPLETED))

        val completed = CustomerRefundStatus.COMPLETED
        assertFalse(completed.canTransitionTo(CustomerRefundStatus.REQUESTED))
    }

    @Test
    fun testValidateAdvanceRecording_Success() {
        val res = CustomerCreditValidator.validateAdvanceRecording(
            tenantId, projectId, customerId, accountId,
            BigDecimal("5000.0000"), "BDT", validAccount
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testValidateAdvanceRecording_ZeroOrNegativeAmount_Fails() {
        val resZero = CustomerCreditValidator.validateAdvanceRecording(
            tenantId, projectId, customerId, accountId,
            BigDecimal.ZERO, "BDT", validAccount
        )
        assertTrue(resZero is DomainResult.Error)

        val resNeg = CustomerCreditValidator.validateAdvanceRecording(
            tenantId, projectId, customerId, accountId,
            BigDecimal("-100.0000"), "BDT", validAccount
        )
        assertTrue(resNeg is DomainResult.Error)
    }

    @Test
    fun testValidateAdvanceRecording_SuspendedAccount_Fails() {
        val suspended = validAccount.copy(status = CustomerFinancialAccountStatus.SUSPENDED)
        val res = CustomerCreditValidator.validateAdvanceRecording(
            tenantId, projectId, customerId, accountId,
            BigDecimal("500.0000"), "BDT", suspended
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testValidateCreditAllocation_ExceedsInvoiceDue_Fails() {
        val res = CustomerCreditValidator.validateCreditAllocation(
            tenantId, projectId, customerId, null,
            BigDecimal("10000.0000"), validInvoice, BigDecimal("2000.0000")
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testValidateCreditAllocation_ExceedsAvailableCredit_Fails() {
        val res = CustomerCreditValidator.validateCreditAllocation(
            tenantId, projectId, customerId, null,
            BigDecimal("500.0000"), validInvoice, BigDecimal("1000.0000")
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testValidateAdjustment_DebitExceedsAvailable_Fails() {
        val res = CustomerCreditValidator.validateAdjustment(
            tenantId, projectId, customerId, accountId,
            BigDecimal("500.0000"), CustomerAdjustmentType.DEBIT, "Debit adjustment",
            validAccount, BigDecimal("200.0000")
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testValidateAdjustment_Credit_Success() {
        val res = CustomerCreditValidator.validateAdjustment(
            tenantId, projectId, customerId, accountId,
            BigDecimal("500.0000"), CustomerAdjustmentType.CREDIT, "Credit adjustment for goodwill",
            validAccount, BigDecimal("200.0000")
        )
        assertTrue(res is DomainResult.Success)
    }
}

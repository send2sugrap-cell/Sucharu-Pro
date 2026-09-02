package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.validation.CustomerPaymentValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentValidationTest {

    @Test
    fun `valid customer payment passes validation`() {
        val payment = CustomerPayment(
            paymentId = "PAY-001",
            paymentNo = "PAY-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = "REC-001",
            amount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            paymentDate = System.currentTimeMillis(),
            status = CustomerPaymentStatus.DRAFT,
            createdBy = "acct-1"
        )

        val result = CustomerPaymentValidator.validatePayment(payment, "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero payment amount throws IllegalArgumentException`() {
        CustomerPayment(
            paymentId = "PAY-001",
            paymentNo = "PAY-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = "REC-001",
            amount = Money.ZERO,
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            paymentDate = System.currentTimeMillis(),
            createdBy = "acct-1"
        )
    }

    @Test
    fun `bank transfer without payment reference fails validation`() {
        val payment = CustomerPayment(
            paymentId = "PAY-001",
            paymentNo = "PAY-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = "REC-001",
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = null,
            paymentDate = System.currentTimeMillis(),
            createdBy = "acct-1"
        )

        val result = CustomerPaymentValidator.validatePayment(payment, "PRJ-01")
        assertTrue(result is DomainResult.Error)
        assertEquals(
            "Payment method 'Bank Transfer / EFT' requires a non-blank payment reference (e.g. Bank Txn ID, Cheque No, Mobile Txn ID).",
            (result as DomainResult.Error).message
        )
    }

    @Test
    fun `valid customer payment receipt passes validation`() {
        val receipt = CustomerPaymentReceipt(
            receiptId = "RCT-001",
            receiptNo = "RCT-2026-001",
            projectId = "PRJ-01",
            paymentId = "PAY-001",
            customerId = "CUST-001",
            receivableId = "REC-001",
            amount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            paymentDate = System.currentTimeMillis(),
            issuedBy = "acct-1"
        )

        val result = CustomerPaymentValidator.validateReceipt(receipt, "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }
}

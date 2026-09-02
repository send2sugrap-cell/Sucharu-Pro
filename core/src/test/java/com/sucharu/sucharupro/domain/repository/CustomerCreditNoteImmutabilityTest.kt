package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerCreditNote
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditNoteImmutabilityTest {

    @Test
    fun `CustomerCreditNote data class is fully immutable`() {
        val cn = CustomerCreditNote(
            creditNoteId = "CN-001",
            creditNoteNo = "CN-2026-0001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            adjustmentId = "ADJ-001",
            financialTransactionId = "FTXN-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            reason = "Immutability check",
            issuedBy = "acct-1",
            issuedAt = 1000L
        )

        assertEquals("CN-001", cn.creditNoteId)
        assertEquals(Money(BigDecimal("5000.00")), cn.amount)
    }
}

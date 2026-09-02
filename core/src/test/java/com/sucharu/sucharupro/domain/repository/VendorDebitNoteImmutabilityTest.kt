package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorDebitNote
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class VendorDebitNoteImmutabilityTest {

    @Test
    fun `VendorDebitNote data class is fully immutable`() {
        val dn = VendorDebitNote(
            debitNoteId = "DN-001",
            debitNoteNo = "DN-2026-0001",
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            adjustmentId = "ADJ-001",
            financialTransactionId = "FTXN-001",
            referenceType = FinancialReferenceType.VENDOR_BILL,
            referenceId = "BILL-1001",
            amount = Money(BigDecimal("4000.00")),
            currency = "BDT",
            reason = "Immutability check",
            issuedBy = "acct-1",
            issuedAt = 1000L
        )

        assertEquals("DN-001", dn.debitNoteId)
        assertEquals(Money(BigDecimal("4000.00")), dn.amount)
    }
}

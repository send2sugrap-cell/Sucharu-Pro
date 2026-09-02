package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.finance.FinancialClosingChecklistCode
import com.sucharu.sucharupro.domain.model.finance.FinancialClosingChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialClosingChecklistTest {

    @Test
    fun `checklist codes contain mandatory verification controls`() {
        val codes = FinancialClosingChecklistCode.entries
        assertTrue(codes.contains(FinancialClosingChecklistCode.LEDGER_BALANCED))
        assertTrue(codes.contains(FinancialClosingChecklistCode.CASH_RECONCILED))
        assertTrue(codes.contains(FinancialClosingChecklistCode.BANK_RECONCILED))
        assertTrue(codes.contains(FinancialClosingChecklistCode.NO_CRITICAL_DISCREPANCIES))
        assertTrue(codes.contains(FinancialClosingChecklistCode.PERIOD_DATES_VALID))

        val item = FinancialClosingChecklistItem(
            code = FinancialClosingChecklistCode.LEDGER_BALANCED,
            isPassed = true,
            details = "Debit == Credit verified"
        )
        assertEquals("General Ledger Debits equal Credits", item.title)
        assertTrue(item.isPassed)
    }
}

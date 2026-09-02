package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationCalculator
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialReconciliationCalculationTest {

    @Test
    fun `calculateReconciliationResult matches zero variance`() {
        val result = FinancialReconciliationCalculator.calculateReconciliationResult(
            expectedAmount = Money(5000.0),
            actualAmount = Money(5000.0)
        )
        assertEquals(FinancialReconciliationStatus.MATCHED, result.status)
        assertTrue(result.difference.isZero())
        assertTrue(result.isWithinTolerance)
    }

    @Test
    fun `calculateReconciliationResult detects mismatch variance`() {
        val result = FinancialReconciliationCalculator.calculateReconciliationResult(
            expectedAmount = Money(5000.0),
            actualAmount = Money(4800.0),
            tolerance = Money.ZERO
        )
        assertEquals(FinancialReconciliationStatus.MISMATCHED, result.status)
        assertEquals(Money(-200.0), result.difference)
    }

    @Test
    fun `calculateExpectedClosingCash calculates arithmetic correctly without floating point`() {
        val expected = FinancialReconciliationCalculator.calculateExpectedClosingCash(
            openingCash = Money(10000.0),
            cashReceipts = Money(2500.50),
            cashPayments = Money(1500.25),
            cashAdjustments = Money(100.0)
        )
        assertEquals(Money(11100.25), expected)
    }

    @Test
    fun `calculateExpectedClosingBank calculates bank reconciliation balance accurately`() {
        val expected = FinancialReconciliationCalculator.calculateExpectedClosingBank(
            openingBank = Money(50000.0),
            deposits = Money(10000.0),
            withdrawals = Money(5000.0),
            outstandingDeposits = Money(2000.0),
            outstandingWithdrawals = Money(3000.0)
        )
        // ledgerBank = 50000 + 10000 - 5000 = 55000
        // expected = 55000 + 3000 - 2000 = 56000
        assertEquals(Money(56000.0), expected)
    }
}

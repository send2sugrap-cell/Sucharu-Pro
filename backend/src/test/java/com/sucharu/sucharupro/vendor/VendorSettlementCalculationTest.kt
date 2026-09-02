package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class VendorSettlementCalculationTest {

    @Test
    fun testOutstandingCalculationZeroSafe() {
        val approved = Money(BigDecimal("10000.00"))
        val settled = Money(BigDecimal("4000.00"))
        val credits = Money(BigDecimal("1000.00"))

        val outstanding = VendorSettlementCalculator.calculateOutstandingAmount(approved, settled, credits)
        assertEquals(Money(BigDecimal("5000.00")), outstanding)

        // When settled > approved, returns 0.00 without negative balance
        val overSettled = Money(BigDecimal("12000.00"))
        val zeroOutstanding = VendorSettlementCalculator.calculateOutstandingAmount(approved, overSettled, credits)
        assertEquals(Money.ZERO, zeroOutstanding)
    }

    @Test
    fun testVarianceCalculation() {
        val expected = Money(BigDecimal("5000.00"))
        val actual = Money(BigDecimal("4800.00"))

        val variance = VendorSettlementCalculator.calculateVariance(expected, actual)
        assertEquals(Money(BigDecimal("200.00")), variance)
    }

    @Test
    fun testRateAndDefectPercentages() {
        assertEquals(80.0, VendorSettlementCalculator.calculateRatePercentage(8.0, 10.0), 0.01)
        assertEquals(100.0, VendorSettlementCalculator.calculateRatePercentage(0.0, 0.0), 0.01)

        assertEquals(5.0, VendorSettlementCalculator.calculateDefectRatePercentage(5.0, 100.0), 0.01)
        assertEquals(0.0, VendorSettlementCalculator.calculateDefectRatePercentage(0.0, 0.0), 0.01)
    }
}

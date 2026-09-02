package com.sucharu.sucharupro.domain.service.printingquote

import com.sucharu.sucharupro.domain.model.printingquote.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class PrintingPricingEngineTest {

    @Test
    fun `compute with COST_PLUS calculates markup and margins correctly`() {
        val totalCost = BigDecimal("10000.0000")
        val unitCost = BigDecimal("10.0000")
        val sellableQty = 1000L
        val assumptions = PricingAssumptions(
            pricingMethod = "COST_PLUS",
            markupPercentage = BigDecimal("25.0000"),
            discountType = DiscountType.NONE,
            taxPercentage = BigDecimal("5.0000")
        )

        val snapshot = PrintingPricingEngine.compute(
            totalCost = totalCost,
            unitCost = unitCost,
            sellableQty = sellableQty,
            assumptions = assumptions
        )

        assertNotNull(snapshot)
        // Unit selling price = 10 * 1.25 = 12.5000
        // Base selling price = 12.5000 * 1000 = 12500.0000
        assertEquals(BigDecimal("12500.0000"), snapshot.baseSellingPrice)
        // Tax = 12500 * 5% = 625.0000
        assertEquals(BigDecimal("625.0000"), snapshot.taxAmount)
        // Final quote total = 12500 + 625 = 13125.0000
        assertEquals(BigDecimal("13125.0000"), snapshot.finalQuoteTotal)
        // Gross Profit = 13125 - 10000 = 3125.0000
        assertEquals(BigDecimal("3125.0000"), snapshot.grossProfit)
        // Break even price = 10000 / 1000 = 10.0000
        assertEquals(BigDecimal("10.0000"), snapshot.breakEvenPrice)
    }

    @Test
    fun `compute with TARGET_MARGIN calculates target price correctly`() {
        val totalCost = BigDecimal("8000.0000")
        val unitCost = BigDecimal("8.0000")
        val sellableQty = 1000L
        val assumptions = PricingAssumptions(
            pricingMethod = "TARGET_MARGIN",
            targetMarginPercentage = BigDecimal("20.0000"),
            discountType = DiscountType.NONE,
            taxPercentage = BigDecimal.ZERO
        )

        val snapshot = PrintingPricingEngine.compute(
            totalCost = totalCost,
            unitCost = unitCost,
            sellableQty = sellableQty,
            assumptions = assumptions
        )

        // Target unit price = 8 / (1 - 0.20) = 10.0000
        // Base selling price = 10000.0000
        assertEquals(BigDecimal("10000.0000"), snapshot.baseSellingPrice)
        assertEquals(BigDecimal("10000.0000"), snapshot.finalQuoteTotal)
        assertEquals(BigDecimal("2000.0000"), snapshot.grossProfit)
        assertEquals(BigDecimal("20.0000"), snapshot.grossMarginPercentage)
    }

    @Test
    fun `reconcile verifies mathematical consistency across all 6 identities`() {
        val totalCost = BigDecimal("10000.0000")
        val unitCost = BigDecimal("10.0000")
        val sellableQty = 1000L
        val assumptions = PricingAssumptions(
            pricingMethod = "COST_PLUS",
            markupPercentage = BigDecimal("20.0000"),
            discountType = DiscountType.NONE,
            taxPercentage = BigDecimal.ZERO
        )

        val snapshot = PrintingPricingEngine.compute(
            totalCost = totalCost,
            unitCost = unitCost,
            sellableQty = sellableQty,
            assumptions = assumptions
        )

        val recon = PrintingPricingEngine.reconcile(
            snapshot = snapshot,
            totalCost = totalCost,
            unitCost = unitCost,
            sellableQty = sellableQty
        )

        assertTrue(recon.isFullyReconciled)
        assertTrue(recon.totalCostCheck)
        assertTrue(recon.revenueIdentityCheck)
        assertTrue(recon.grossProfitCheck)
        assertTrue(recon.marginCheck)
        assertTrue(recon.markupCheck)
        assertTrue(recon.breakevenCheck)
    }
}

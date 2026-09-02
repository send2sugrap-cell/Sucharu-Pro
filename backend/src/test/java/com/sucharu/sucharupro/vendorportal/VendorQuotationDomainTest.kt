package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.VendorQuotationItem
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorQuotationCalculator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorQuotationDomainTest {

    @Test
    fun testQuotationLineTotalCalculation() {
        val lineTotal = VendorQuotationCalculator.calculateLineTotal(
            quantity = BigDecimal("10.00"),
            unitPrice = Money("100.00"),
            discountAmount = Money("50.00"),
            taxAmount = Money("20.00")
        )
        assertEquals(Money("970.00"), lineTotal)
    }

    @Test
    fun testQuotationTotalsCalculation() {
        val items = listOf(
            VendorQuotationItem(
                quotationItemId = "qi-1",
                quotationId = "q-1",
                rfqItemId = "rfq-item-1",
                quantity = BigDecimal("2.00"),
                unitPrice = Money("500.00"),
                discountAmount = Money("50.00"),
                taxAmount = Money("25.00"),
                lineTotal = Money("975.00")
            ),
            VendorQuotationItem(
                quotationItemId = "qi-2",
                quotationId = "q-1",
                rfqItemId = "rfq-item-2",
                quantity = BigDecimal("5.00"),
                unitPrice = Money("200.00"),
                discountAmount = Money("0.00"),
                taxAmount = Money("50.00"),
                lineTotal = Money("1050.00")
            )
        )

        val totals = VendorQuotationCalculator.calculateQuotationTotals(items)
        assertEquals(Money("2000.00"), totals.subtotal)
        assertEquals(Money("50.00"), totals.totalDiscount)
        assertEquals(Money("75.00"), totals.totalTax)
        assertEquals(Money("2025.00"), totals.grandTotal)
    }
}

package com.sucharu.sucharupro.domain.validation.customerinvoice

import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic Financial Calculation Engine for Customer Invoices (Module 14 Step 02).
 */
object CustomerInvoiceCalculator {

    data class CalculatedTotals(
        val subtotal: BigDecimal,
        val discount: BigDecimal,
        val tax: BigDecimal,
        val adjustment: BigDecimal,
        val grandTotal: BigDecimal,
        val paidAmount: BigDecimal,
        val dueAmount: BigDecimal
    )

    fun calculateLineTotal(
        quantity: BigDecimal,
        unitPrice: BigDecimal,
        lineDiscount: BigDecimal = BigDecimal.ZERO,
        lineTax: BigDecimal = BigDecimal.ZERO
    ): BigDecimal {
        val base = quantity.multiply(unitPrice).setScale(4, RoundingMode.HALF_UP)
        val afterDiscount = base.subtract(lineDiscount).max(BigDecimal.ZERO)
        return afterDiscount.add(lineTax).setScale(4, RoundingMode.HALF_UP)
    }

    fun calculateInvoiceTotals(
        lines: List<CustomerInvoiceLine>,
        invoiceDiscount: BigDecimal = BigDecimal.ZERO,
        invoiceTax: BigDecimal = BigDecimal.ZERO,
        adjustment: BigDecimal = BigDecimal.ZERO,
        paidAmount: BigDecimal = BigDecimal.ZERO
    ): CalculatedTotals {
        val subtotal = lines.fold(BigDecimal.ZERO) { acc, line ->
            acc.add(line.lineTotal)
        }.setScale(4, RoundingMode.HALF_UP)

        val normalizedDiscount = invoiceDiscount.setScale(4, RoundingMode.HALF_UP)
        val normalizedTax = invoiceTax.setScale(4, RoundingMode.HALF_UP)
        val normalizedAdjustment = adjustment.setScale(4, RoundingMode.HALF_UP)
        val normalizedPaid = paidAmount.setScale(4, RoundingMode.HALF_UP)

        val grandTotal = subtotal
            .subtract(normalizedDiscount)
            .add(normalizedTax)
            .add(normalizedAdjustment)
            .max(BigDecimal.ZERO)
            .setScale(4, RoundingMode.HALF_UP)

        val dueAmount = grandTotal
            .subtract(normalizedPaid)
            .max(BigDecimal.ZERO)
            .setScale(4, RoundingMode.HALF_UP)

        return CalculatedTotals(
            subtotal = subtotal,
            discount = normalizedDiscount,
            tax = normalizedTax,
            adjustment = normalizedAdjustment,
            grandTotal = grandTotal,
            paidAmount = normalizedPaid,
            dueAmount = dueAmount
        )
    }
}

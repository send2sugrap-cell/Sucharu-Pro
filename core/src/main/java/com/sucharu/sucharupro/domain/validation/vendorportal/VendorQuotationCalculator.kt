package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic, zero-safe calculation engine for Vendor Quotations & RFQ Evaluations (Module 13 Step 03).
 */
object VendorQuotationCalculator {

    /**
     * Calculates line total for a single quotation item:
     * lineTotal = (quantity * unitPrice) - discountAmount + taxAmount
     */
    fun calculateLineTotal(
        quantity: BigDecimal,
        unitPrice: Money,
        discountAmount: Money = Money.ZERO,
        taxAmount: Money = Money.ZERO
    ): Money {
        if (quantity <= BigDecimal.ZERO || unitPrice.amount <= BigDecimal.ZERO) {
            return Money.ZERO
        }
        val gross = unitPrice * quantity
        val afterDiscount = if (discountAmount.amount > BigDecimal.ZERO) {
            val d = gross - discountAmount
            if (d.amount < BigDecimal.ZERO) Money.ZERO else d
        } else {
            gross
        }
        return afterDiscount + taxAmount
    }

    /**
     * Computes the aggregated financial summary for a collection of quotation items.
     */
    fun calculateQuotationTotals(items: List<VendorQuotationItem>): QuotationFinancialSummary {
        var subtotal = Money.ZERO
        var totalDiscount = Money.ZERO
        var totalTax = Money.ZERO
        var grandTotal = Money.ZERO

        for (item in items) {
            val gross = item.unitPrice * item.quantity
            subtotal = subtotal + gross
            totalDiscount = totalDiscount + item.discountAmount
            totalTax = totalTax + item.taxAmount
            grandTotal = grandTotal + item.lineTotal
        }

        return QuotationFinancialSummary(
            subtotal = subtotal,
            totalDiscount = totalDiscount,
            totalTax = totalTax,
            grandTotal = grandTotal
        )
    }

    /**
     * Computes weighted evaluation score:
     * weightedScore = (rawScore * weightPercent) / 100.0
     */
    fun calculateWeightedScore(rawScore: Double, weightPercent: Double): Double {
        if (rawScore <= 0.0 || weightPercent <= 0.0) return 0.0
        val clampedRaw = rawScore.coerceIn(0.0, 100.0)
        val clampedWeight = weightPercent.coerceIn(0.0, 100.0)
        val raw = (clampedRaw * clampedWeight) / 100.0
        return BigDecimal(raw).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Aggregates total evaluation score across all weighted criteria.
     */
    fun calculateTotalEvaluationScore(scores: List<VendorRfqEvaluationScore>): Double {
        if (scores.isEmpty()) return 0.0
        val sum = scores.sumOf { it.weightedScore }
        return BigDecimal(sum.coerceIn(0.0, 100.0)).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Generates a comparison snapshot across all submitted quotations for an RFQ.
     */
    fun generateComparisonSnapshot(
        rfq: VendorRfq,
        totalInvited: Int,
        quotations: List<VendorQuotation>,
        evaluations: Map<String, VendorRfqEvaluation>,
        vendorInfoMap: Map<String, Pair<String, String>> // vendorId -> (code, name)
    ): VendorRfqComparisonSnapshot {
        val submittedQuotations = quotations.filter { it.status.isSubmittedOrActive }
        val items = submittedQuotations.map { q ->
            val info = vendorInfoMap[q.vendorId] ?: Pair("VND-UNKNOWN", "Unknown Vendor")
            val eval = evaluations[q.quotationId]
            VendorRfqComparisonItem(
                quotationId = q.quotationId,
                vendorId = q.vendorId,
                vendorCode = info.first,
                vendorName = info.second,
                grandTotal = q.grandTotal,
                deliveryLeadTimeDays = q.deliveryLeadTimeDays,
                evaluationScore = eval?.totalScore,
                decision = eval?.decision,
                submittedAt = q.submittedAt
            )
        }.sortedBy { it.grandTotal.amount }

        val lowest = items.minByOrNull { it.grandTotal.amount }?.grandTotal
        val highest = items.maxByOrNull { it.grandTotal.amount }?.grandTotal
        val avg = if (items.isNotEmpty()) {
            val totalAmount = items.map { it.grandTotal.amount }.reduce { acc, bigDecimal -> acc.add(bigDecimal) }
            val avgBd = totalAmount.divide(BigDecimal.valueOf(items.size.toLong()), 2, RoundingMode.HALF_UP)
            Money(avgBd)
        } else null

        return VendorRfqComparisonSnapshot(
            rfqId = rfq.rfqId,
            rfqNumber = rfq.rfqNumber,
            title = rfq.title,
            totalInvited = totalInvited,
            totalBidsReceived = items.size,
            lowestBidAmount = lowest,
            highestBidAmount = highest,
            averageBidAmount = avg,
            comparisonItems = items
        )
    }
}

data class QuotationFinancialSummary(
    val subtotal: Money,
    val totalDiscount: Money,
    val totalTax: Money,
    val grandTotal: Money
)

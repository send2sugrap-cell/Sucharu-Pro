package com.sucharu.sucharupro.domain.service.printingquote

import com.sucharu.sucharupro.domain.model.printingquote.*
import java.math.BigDecimal

/**
 * Pure, stateless pricing computation engine for the Smart Printing Calculator quotation layer.
 *
 * Produces a [PrintingPricingSnapshot] from a [unitCost], [totalCost], and [PricingAssumptions].
 *
 * Pricing methods:
 *  - COST_PLUS: sellingPrice = unitCost × (1 + markupPct / 100)
 *  - TARGET_MARGIN: sellingPrice = unitCost / (1 − targetMarginPct / 100)
 *  - MANUAL: sellingPrice passed directly as [PricingAssumptions.manualUnitPrice]
 *
 * All identities verified:
 *  - grossProfit  = revenue − totalCost
 *  - grossMargin% = grossProfit / revenue × 100
 *  - markup%      = (sellingPrice − unitCost) / unitCost × 100
 *  - breakEvenPx  = totalCost / sellableQty
 *  - revenueId    = finalTotal = sellPrice × sellableQty (before discount/tax; after applied correctly)
 *
 * Zero-safe: all divisions use [safeDiv]. No floating-point arithmetic.
 * No I/O — deterministic for given inputs.
 * Module 17 Step 02.
 */
object PrintingPricingEngine {

    private const val ENGINE_VERSION = "2.0.0"

    // ─────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────

    /**
     * Compute a complete [PrintingPricingSnapshot] for the given cost and assumptions.
     *
     * @param totalCost      Total direct + overhead cost (from [PrintingCostingEngine])
     * @param unitCost       totalCost / sellableQuantity
     * @param sellableQty    Sellable quantity (units)
     * @param assumptions    Pricing assumptions (method, markup %, tax, discount, etc.)
     */
    fun compute(
        totalCost: BigDecimal,
        unitCost: BigDecimal,
        sellableQty: Long,
        assumptions: PricingAssumptions
    ): PrintingPricingSnapshot {
        val qty = BigDecimal(sellableQty)

        // 1. Base selling price per unit
        val baseUnitPrice = computeSellingPrice(unitCost, assumptions)
        val baseSellingPrice = baseUnitPrice.multiply(qty).q4()   // pre-discount total

        // 2. Discount
        val (discountAmount, priceAfterDiscount) = applyDiscount(baseSellingPrice, assumptions)

        // 3. Tax
        val taxAmount = priceAfterDiscount.multiply(assumptions.taxPercentage)
            .safeDiv(QUOTE_ONE_HUNDRED).q4()
        val finalQuoteTotal = (priceAfterDiscount + taxAmount).q4()

        // 4. Margin intelligence (always based on final revenue vs total cost)
        val revenue = finalQuoteTotal
        val grossProfit = (revenue - totalCost).q4()
        val grossMarginPct = grossProfit.safeDiv(revenue).multiply(QUOTE_ONE_HUNDRED).q4()
        val markupAmount = (baseUnitPrice - unitCost).q4()
        val markupPct = markupAmount.safeDiv(unitCost).multiply(QUOTE_ONE_HUNDRED).q4()

        // Contribution margin (assumes variable cost = totalCost, no fixed split at this layer)
        val contributionAmount = grossProfit
        val contributionMarginPct = grossMarginPct

        // 5. Break-even
        val breakEvenPrice = totalCost.safeDiv(qty).q4()
        val breakEvenQty = computeBreakEvenQuantity(totalCost, baseUnitPrice)

        // 6. Target margin price (informational)
        val (targetMarginPrice, targetMarginPct) = computeTargetMarginPrice(unitCost, assumptions)

        return PrintingPricingSnapshot(
            baseSellingPrice = baseSellingPrice,
            discountType = assumptions.discountType,
            discountValue = assumptions.discountValue,
            discountAmount = discountAmount,
            taxPercentage = assumptions.taxPercentage,
            taxAmount = taxAmount,
            finalQuoteTotal = finalQuoteTotal,
            markupAmount = markupAmount.multiply(qty).q4(),
            markupPercentage = markupPct,
            grossProfit = grossProfit,
            grossMarginPercentage = grossMarginPct,
            contributionAmount = contributionAmount,
            contributionMarginPercentage = contributionMarginPct,
            breakEvenPrice = breakEvenPrice,
            breakEvenQuantity = breakEvenQty,
            targetMarginPrice = targetMarginPrice,
            targetMarginPercentage = targetMarginPct
        )
    }

    /**
     * Compute the per-unit selling price according to the pricing method in [assumptions].
     * Called by [PrintingCostingEngine] for tier pricing as well.
     */
    fun computeSellingPrice(unitCost: BigDecimal, assumptions: PricingAssumptions): BigDecimal {
        return when (assumptions.pricingMethod) {
            "TARGET_MARGIN" -> {
                val margin = assumptions.targetMarginPercentage
                val denominator = (QUOTE_ONE_HUNDRED - margin).safeDiv(QUOTE_ONE_HUNDRED)
                unitCost.safeDiv(denominator)
            }
            "MANUAL" -> assumptions.manualUnitPrice ?: unitCost
            else -> {
                // COST_PLUS (default)
                val factor = QUOTE_ONE_HUNDRED + assumptions.markupPercentage
                unitCost.multiply(factor).safeDiv(QUOTE_ONE_HUNDRED)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Reconciliation check: verifies all 6 financial identities
    // ─────────────────────────────────────────────────────────────

    data class ReconciliationResult(
        val totalCostCheck: Boolean,
        val revenueIdentityCheck: Boolean,
        val grossProfitCheck: Boolean,
        val marginCheck: Boolean,
        val markupCheck: Boolean,
        val breakevenCheck: Boolean,
        val isFullyReconciled: Boolean,
        val discrepancies: List<String>
    )

    fun reconcile(
        snapshot: PrintingPricingSnapshot,
        totalCost: BigDecimal,
        unitCost: BigDecimal,
        sellableQty: Long
    ): ReconciliationResult {
        val discrepancies = mutableListOf<String>()
        val tolerance = BigDecimal("0.0100")  // 1 paisa / 1 cent tolerance for rounding
        val qty = BigDecimal(sellableQty)

        // 1. Cost check: unitCost × qty ≈ totalCost
        val reconCost = unitCost.multiply(qty).q4()
        val totalCostCheck = (reconCost - totalCost).abs() <= tolerance
        if (!totalCostCheck) discrepancies += "TotalCost identity: ${reconCost.toPlainString()} ≠ ${totalCost.toPlainString()}"

        // 2. Revenue identity: (baseSellingPrice - discountAmount + taxAmount) ≈ finalQuoteTotal
        val reconRevenue = (snapshot.baseSellingPrice - snapshot.discountAmount + snapshot.taxAmount).q4()
        val revenueIdentityCheck = (reconRevenue - snapshot.finalQuoteTotal).abs() <= tolerance
        if (!revenueIdentityCheck) discrepancies += "Revenue identity: ${reconRevenue.toPlainString()} ≠ ${snapshot.finalQuoteTotal.toPlainString()}"

        // 3. Gross profit check: finalTotal - totalCost ≈ grossProfit
        val reconGP = (snapshot.finalQuoteTotal - totalCost).q4()
        val grossProfitCheck = (reconGP - snapshot.grossProfit).abs() <= tolerance
        if (!grossProfitCheck) discrepancies += "GrossProfit: ${reconGP.toPlainString()} ≠ ${snapshot.grossProfit.toPlainString()}"

        // 4. Margin check: grossProfit / finalTotal × 100 ≈ grossMarginPct
        val reconMargin = snapshot.grossProfit.safeDiv(snapshot.finalQuoteTotal).multiply(QUOTE_ONE_HUNDRED).q4()
        val marginCheck = (reconMargin - snapshot.grossMarginPercentage).abs() <= tolerance
        if (!marginCheck) discrepancies += "GrossMargin%: ${reconMargin.toPlainString()} ≠ ${snapshot.grossMarginPercentage.toPlainString()}"

        // 5. Markup check: markupAmount / (unitCost × qty) × 100 ≈ markupPct
        val costBase = unitCost.multiply(qty).q4()
        val reconMarkup = snapshot.markupAmount.safeDiv(costBase).multiply(QUOTE_ONE_HUNDRED).q4()
        val markupCheck = (reconMarkup - snapshot.markupPercentage).abs() <= tolerance
        if (!markupCheck) discrepancies += "Markup%: ${reconMarkup.toPlainString()} ≠ ${snapshot.markupPercentage.toPlainString()}"

        // 6. Break-even check: breakEvenPrice × qty ≈ totalCost
        val reconBE = snapshot.breakEvenPrice.multiply(qty).q4()
        val breakevenCheck = (reconBE - totalCost).abs() <= tolerance
        if (!breakevenCheck) discrepancies += "BreakEven: ${reconBE.toPlainString()} × $sellableQty ≠ ${totalCost.toPlainString()}"

        return ReconciliationResult(
            totalCostCheck = totalCostCheck,
            revenueIdentityCheck = revenueIdentityCheck,
            grossProfitCheck = grossProfitCheck,
            marginCheck = marginCheck,
            markupCheck = markupCheck,
            breakevenCheck = breakevenCheck,
            isFullyReconciled = discrepancies.isEmpty(),
            discrepancies = discrepancies
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────

    private fun applyDiscount(
        baseTotal: BigDecimal,
        assumptions: PricingAssumptions
    ): Pair<BigDecimal, BigDecimal> {
        return when (assumptions.discountType) {
            DiscountType.FIXED_AMOUNT -> {
                val disc = assumptions.discountValue.q4()
                    .coerceAtMost(baseTotal)
                disc to (baseTotal - disc).q4()
            }
            DiscountType.PERCENTAGE -> {
                val disc = baseTotal.multiply(assumptions.discountValue)
                    .safeDiv(QUOTE_ONE_HUNDRED).q4()
                disc to (baseTotal - disc).q4()
            }
            else -> QUOTE_ZERO to baseTotal
        }
    }

    private fun computeBreakEvenQuantity(totalCost: BigDecimal, sellingPricePerUnit: BigDecimal): Long {
        if (sellingPricePerUnit <= QUOTE_ZERO) return 0L
        return totalCost.safeDiv(sellingPricePerUnit).toLong().coerceAtLeast(0L)
    }

    private fun computeTargetMarginPrice(
        unitCost: BigDecimal,
        assumptions: PricingAssumptions
    ): Pair<BigDecimal?, BigDecimal?> {
        val targetMargin = assumptions.targetMarginPercentage
        if (targetMargin <= QUOTE_ZERO || targetMargin >= QUOTE_ONE_HUNDRED) return null to null
        val denominator = (QUOTE_ONE_HUNDRED - targetMargin).safeDiv(QUOTE_ONE_HUNDRED)
        val price = unitCost.safeDiv(denominator).q4()
        return price to targetMargin
    }
}

// Extension to support manual pricing on PricingAssumptions
val PricingAssumptions.manualUnitPrice: BigDecimal?
    get() = null  // Optional: extended via UI overrides; default null means no manual override

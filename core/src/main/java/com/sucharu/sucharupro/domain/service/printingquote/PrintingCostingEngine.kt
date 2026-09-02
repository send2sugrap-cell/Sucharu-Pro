package com.sucharu.sucharupro.domain.service.printingquote

import com.sucharu.sucharupro.domain.model.printingquote.*
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import java.math.BigDecimal
import java.util.UUID

/**
 * Pure, stateless costing computation engine for the Smart Printing Calculator quotation layer.
 *
 * Consumes a Step 01 [PrintingCalculationResult] and explicit overhead assumptions, then
 * produces a list of [PrintingCostComponent] and the canonical [totalCost] / [unitCost].
 *
 * Rules:
 *  - Zero-safe: all divisions guarded via [safeDiv].
 *  - No I/O: deterministic for given inputs.
 *  - Scale = 4, HALF_UP throughout.
 *  - Does NOT set prices — pricing is the exclusive responsibility of [PrintingPricingEngine].
 *
 * Module 17 Step 02.
 */
object PrintingCostingEngine {

    private const val ENGINE_VERSION = "2.0.0"

    // ─────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────

    data class CostingResult(
        val components: List<PrintingCostComponent>,
        val totalCost: BigDecimal,
        val unitCost: BigDecimal,
        val engineVersion: String = ENGINE_VERSION
    )

    /**
     * Derives cost components directly from a Step 01 calculation result.
     *
     * Quantity basis = the SELLABLE quantity from [quantityBreakdown].
     * Cost per unit = totalCost / sellableQuantity.
     */
    fun computeFromStep01(
        quoteId: String,
        versionId: String,
        step01: PrintingCalculationResult,
        quantityBreakdown: QuoteQuantityBreakdown,
        assumptions: CostingAssumptions
    ): CostingResult {
        val components = mutableListOf<PrintingCostComponent>()
        var sortOrder = 0

        // 1. Paper / Substrate cost
        val materialCost = step01.materialRequirement.estimatedMaterialCost.q4OrZero()
        if (materialCost > QUOTE_ZERO || assumptions.wastageCosted) {
            components += newComponent(
                quoteId, versionId, CostComponentType.PAPER,
                "PAPER_COST", "Paper / Substrate Cost",
                quantity = quantityBreakdown.totalSheetsAsDecimal(),
                unit = "sheets",
                unitRate = step01.materialRequirement.estimatedMaterialCost
                    ?.let { it.safeDiv(BigDecimal(step01.materialRequirement.totalSheetsRequired)) },
                amount = materialCost,
                formula = "totalSheets × unitPricePerSheet",
                sourceRef = "STEP01_MATERIAL",
                sortOrder = sortOrder++
            )
        }

        // 2. Printing machine cost
        val printingCost = step01.printingRequirement.estimatedPrintingCost.q4OrZero()
        if (printingCost > QUOTE_ZERO) {
            components += newComponent(
                quoteId, versionId, CostComponentType.MACHINE,
                "MACHINE_COST", "Printing Machine Cost",
                quantity = BigDecimal(step01.printingRequirement.totalImpressions),
                unit = "impressions",
                unitRate = null,
                amount = printingCost,
                formula = "totalImpressions × machineHourlyRate / impressionsPerHour",
                sourceRef = "STEP01_PRINTING",
                sortOrder = sortOrder++
            )
        }

        // 3. Plate / Prepress cost
        val plateCost = step01.printingRequirement.estimatedPlateCost.q4OrZero()
        if (plateCost > QUOTE_ZERO) {
            components += newComponent(
                quoteId, versionId, CostComponentType.PLATE,
                "PLATE_COST", "Plate / Prepress Cost",
                quantity = BigDecimal(step01.printingRequirement.plateCount),
                unit = "plates",
                unitRate = plateCost.safeDiv(BigDecimal(maxOf(step01.printingRequirement.plateCount, 1))),
                amount = plateCost,
                formula = "plateCount × plateCostPerUnit",
                sourceRef = "STEP01_PLATE",
                sortOrder = sortOrder++
            )
        }

        // 4. Finishing operations (mapped from Step 01 breakdown items)
        val finishingCost = step01.finishingRequirement.totalEstimatedFinishingCost.q4OrZero()
        if (finishingCost > QUOTE_ZERO) {
            components += newComponent(
                quoteId, versionId, CostComponentType.FINISHING,
                "FINISHING_COST", "Finishing Operations Cost",
                quantity = BigDecimal(step01.finishingRequirement.operations.size),
                unit = "operations",
                unitRate = null,
                amount = finishingCost,
                formula = "sum(finishingOperations.calculatedAmount)",
                sourceRef = "STEP01_FINISHING",
                sortOrder = sortOrder++
            )
        }

        // 5. Wastage cost (setup + running + finishing sheets, valued at material unit rate)
        if (assumptions.wastageCosted) {
            val wastageSheets = step01.materialRequirement.wasteSheetsRequired
            val unitPrice = step01.materialRequirement.estimatedMaterialCost
                ?.let { it.safeDiv(BigDecimal(step01.materialRequirement.totalSheetsRequired)) }
                ?: QUOTE_ZERO
            val wastageCost = unitPrice.multiply(BigDecimal(wastageSheets)).q4()
            if (wastageCost > QUOTE_ZERO) {
                components += newComponent(
                    quoteId, versionId, CostComponentType.WASTAGE,
                    "WASTAGE_COST", "Wastage Material Cost",
                    quantity = BigDecimal(wastageSheets),
                    unit = "waste sheets",
                    unitRate = unitPrice,
                    amount = wastageCost,
                    formula = "wasteSheets × materialUnitPrice",
                    sourceRef = "STEP01_WASTAGE",
                    sortOrder = sortOrder++
                )
            }
        }

        // 6. Overhead allocation
        val directTotal = components.sumOf { it.amount }
        if (assumptions.overheadAllocationPct > QUOTE_ZERO) {
            val overheadAmt = directTotal.multiply(assumptions.overheadAllocationPct)
                .safeDiv(QUOTE_ONE_HUNDRED).q4()
            components += newComponent(
                quoteId, versionId, CostComponentType.OVERHEAD,
                "OVERHEAD", "Overhead Allocation",
                quantity = QUOTE_ONE_HUNDRED,
                unit = "percent",
                unitRate = null,
                amount = overheadAmt,
                formula = "directCosts × overheadPct / 100",
                sortOrder = sortOrder++
            )
        }

        val totalCost = components.sumOf { it.amount }.q4()
        val sellableQty = BigDecimal(quantityBreakdown.sellableQuantity)
        val unitCost = totalCost.safeDiv(sellableQty)

        return CostingResult(components = components, totalCost = totalCost, unitCost = unitCost)
    }

    /**
     * Computes tiered quantity pricing: for each tier quantity, scale material + machine costs
     * proportionally, keeping fixed costs (setup, plate, overhead) constant.
     */
    fun computeQuantityTiers(
        quoteId: String,
        versionId: String,
        baseTotalCost: BigDecimal,
        baseQuantity: Long,
        step01: PrintingCalculationResult,
        tierQuantities: List<Long>,
        pricingAssumptions: PricingAssumptions
    ): List<PrintingQuantityTier> {
        if (tierQuantities.isEmpty()) return emptyList()

        val fixedCost = (step01.printingRequirement.estimatedPlateCost.q4OrZero())
        val variableBase = (baseTotalCost - fixedCost).coerceAtLeast(QUOTE_ZERO)

        return tierQuantities.map { qty ->
            val scale = BigDecimal(qty).safeDiv(BigDecimal(baseQuantity))
            val scaledVariable = variableBase.multiply(scale).q4()
            val tierCost = (fixedCost + scaledVariable).q4()
            val tierUnitCost = tierCost.safeDiv(BigDecimal(qty))
            val sellingUnitPrice = PrintingPricingEngine.computeSellingPrice(
                unitCost = tierUnitCost,
                assumptions = pricingAssumptions
            )
            val tierTotal = sellingUnitPrice.multiply(BigDecimal(qty)).q4()
            val tierGrossMargin = (tierTotal - tierCost).safeDiv(tierTotal)
                .multiply(QUOTE_ONE_HUNDRED).q4()

            PrintingQuantityTier(
                tierId = UUID.randomUUID().toString(),
                versionId = versionId,
                quoteId = quoteId,
                tierQuantity = qty,
                unitCost = tierUnitCost,
                totalCost = tierCost,
                sellingPricePerUnit = sellingUnitPrice,
                finalTotal = tierTotal,
                grossMarginPercentage = tierGrossMargin,
                isBaseTier = qty == baseQuantity
            )
        }.sortedBy { it.tierQuantity }
    }

    // ─────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────

    private fun QuoteQuantityBreakdown.totalSheetsAsDecimal(): BigDecimal =
        BigDecimal(producedQuantity + wastageQuantity)

    private fun newComponent(
        quoteId: String, versionId: String,
        type: CostComponentType, code: String, description: String,
        quantity: BigDecimal, unit: String,
        unitRate: BigDecimal?, amount: BigDecimal,
        formula: String, sourceRef: String? = null, sortOrder: Int = 0
    ): PrintingCostComponent = PrintingCostComponent(
        componentId = UUID.randomUUID().toString(),
        versionId = versionId,
        quoteId = quoteId,
        componentType = type,
        componentCode = code,
        description = description,
        quantity = quantity.q4(),
        unit = unit,
        unitRate = unitRate?.q4(),
        amount = amount.q4(),
        formulaReference = formula,
        sourceRef = sourceRef,
        isApplicable = true,
        sortOrder = sortOrder
    )
}

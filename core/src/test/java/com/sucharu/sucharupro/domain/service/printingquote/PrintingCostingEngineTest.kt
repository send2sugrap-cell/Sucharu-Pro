package com.sucharu.sucharupro.domain.service.printingquote

import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.model.printingquote.*
import com.sucharu.sucharupro.domain.model.product.ProductType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class PrintingCostingEngineTest {

    private fun createDummyCalculationResult(): PrintingCalculationResult {
        val now = System.currentTimeMillis()
        val dim = PrintingDimension(
            width = BigDecimal("210.0000"),
            height = BigDecimal("297.0000"),
            unit = MeasurementUnit.MILLIMETERS
        )
        val sheetDim = PrintingDimension(
            width = BigDecimal("635.0000"),
            height = BigDecimal("914.0000"),
            unit = MeasurementUnit.MILLIMETERS
        )
        val normSpec = NormalizedPrintingSpecification(
            jobTitle = "Test Brochure",
            productType = ProductType.PRINTING_JOB,
            finishedDimension = dim,
            normalizedDimensionMm = dim,
            quantity = QuantitySpecification(
                orderedQuantity = 1000L,
                unit = QuantityUnit.PIECES
            ),
            material = PaperMaterialSpecification(
                materialName = "Art Paper 150gsm",
                stockType = PaperStockType.ART_PAPER,
                sheetDimension = sheetDim,
                unitPricePerSheet = BigDecimal("8.5000")
            ),
            processType = PrintingProcessType.OFFSET,
            sides = PrintingSideOption.DOUBLE_SIDED_SAME,
            color = ColorSpecification(
                colorMode = ColorMode.CMYK_FOUR_COLOR,
                frontColorsCount = 4,
                backColorsCount = 4
            )
        )

        return PrintingCalculationResult(
            calculationId = "CALC-001",
            tenantId = "TENANT-001",
            projectId = "PROJ-001",
            requestFingerprint = "FP-REQ-001",
            requestedAt = now,
            calculatedAt = now,
            status = CalculationStatus.SUCCESSFUL,
            classification = EstimateActualClassification.ESTIMATED,
            normalizedSpecification = normSpec,
            materialRequirement = MaterialRequirementResult(
                finishedItemsPerSheet = 8,
                cutDirection = "STANDARD_GRID",
                productiveSheetsRequired = 500L,
                wasteSheetsRequired = 150L,
                totalSheetsRequired = 650L,
                totalReamsRequired = BigDecimal("1.3000"),
                totalWeightKg = BigDecimal("25.0000"),
                estimatedMaterialCost = BigDecimal("5525.0000"),
                costStatus = CalculationStatus.SUCCESSFUL
            ),
            printingRequirement = PrintingRequirementResult(
                totalImpressions = 1300L,
                totalPasses = 2,
                plateCount = 4,
                estimatedPrintingCost = BigDecimal("2500.0000"),
                estimatedPlateCost = BigDecimal("1200.0000"),
                costStatus = CalculationStatus.SUCCESSFUL
            ),
            finishingRequirement = FinishingRequirementResult(
                operations = listOf(
                    CalculationBreakdownItem(
                        componentCode = "CUTTING_TRIMMING",
                        description = "Guillotine Cutting",
                        quantity = BigDecimal("1000.0000"),
                        unit = "PIECES",
                        unitRate = BigDecimal("0.4500"),
                        calculatedAmount = BigDecimal("450.0000"),
                        formulaReference = "qty × unitRate"
                    )
                ),
                totalEstimatedFinishingCost = BigDecimal("450.0000"),
                costStatus = CalculationStatus.SUCCESSFUL
            ),
            breakdownItems = emptyList(),
            totalEstimatedCost = BigDecimal("9675.0000"),
            estimatedUnitCost = BigDecimal("9.6750"),
            currency = "BDT",
            diagnostics = emptyList(),
            integrityHash = "HASH-CALC-001"
        )
    }

    @Test
    fun `computeFromStep01 derives cost components correctly`() {
        val step01 = createDummyCalculationResult()
        val qtyBreakdown = QuoteQuantityBreakdown(
            orderedQuantity = 1000L,
            producedQuantity = 1000L,
            sellableQuantity = 1000L,
            wastageQuantity = 150L,
            wastagePercentage = BigDecimal("15.0000"),
            impositionUps = 8
        )
        val assumptions = CostingAssumptions(
            overheadAllocationPct = BigDecimal("10.0000"),
            wastageCosted = true
        )

        val result = PrintingCostingEngine.computeFromStep01(
            quoteId = "QUO-001",
            versionId = "VER-001",
            step01 = step01,
            quantityBreakdown = qtyBreakdown,
            assumptions = assumptions
        )

        assertNotNull(result)
        assertTrue(result.components.isNotEmpty())
        assertTrue(result.totalCost > BigDecimal.ZERO)
        assertTrue(result.unitCost > BigDecimal.ZERO)

        // Verify totalCost = sum of components
        val sumOfComponents = result.components.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
        assertEquals(sumOfComponents.setScale(4), result.totalCost.setScale(4))

        // Verify paper component exists
        val paperComp = result.components.find { it.componentType == CostComponentType.PAPER }
        assertNotNull(paperComp)
        assertEquals(BigDecimal("5525.0000"), paperComp?.amount)

        // Verify overhead component exists
        val overheadComp = result.components.find { it.componentType == CostComponentType.OVERHEAD }
        assertNotNull(overheadComp)
    }

    @Test
    fun `computeFromStep01 with zero overhead does not add overhead component`() {
        val step01 = createDummyCalculationResult()
        val qtyBreakdown = QuoteQuantityBreakdown(
            orderedQuantity = 1000L,
            producedQuantity = 1000L,
            sellableQuantity = 1000L,
            wastageQuantity = 0L,
            wastagePercentage = BigDecimal.ZERO,
            impositionUps = 8
        )
        val assumptions = CostingAssumptions(
            overheadAllocationPct = BigDecimal.ZERO,
            wastageCosted = false
        )

        val result = PrintingCostingEngine.computeFromStep01(
            quoteId = "QUO-001",
            versionId = "VER-001",
            step01 = step01,
            quantityBreakdown = qtyBreakdown,
            assumptions = assumptions
        )

        val overheadComp = result.components.find { it.componentType == CostComponentType.OVERHEAD }
        assertNull(overheadComp)
    }

    @Test
    fun `computeQuantityTiers computes scale volume pricing for all tiers`() {
        val step01 = createDummyCalculationResult()
        val baseTierQuantity = 1000L
        val baseTotalCost = BigDecimal("9675.0000")
        val tierQuantities = listOf(500L, 1000L, 2000L, 5000L)
        val assumptions = PricingAssumptions(
            pricingMethod = "COST_PLUS",
            markupPercentage = BigDecimal("30.0000")
        )

        val tiers = PrintingCostingEngine.computeQuantityTiers(
            quoteId = "QUO-001",
            versionId = "VER-001",
            baseTotalCost = baseTotalCost,
            baseQuantity = baseTierQuantity,
            step01 = step01,
            tierQuantities = tierQuantities,
            pricingAssumptions = assumptions
        )

        assertEquals(4, tiers.size)
        val baseTier = tiers.find { it.tierQuantity == 1000L }
        assertNotNull(baseTier)
        assertTrue(baseTier!!.isBaseTier)

        val highVolumeTier = tiers.find { it.tierQuantity == 5000L }
        assertNotNull(highVolumeTier)
        assertTrue(highVolumeTier!!.unitCost < baseTier.unitCost)
    }
}

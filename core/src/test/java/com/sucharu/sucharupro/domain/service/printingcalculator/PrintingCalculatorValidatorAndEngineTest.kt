package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.model.product.ProductType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Validation and Engine Tests for Smart Printing Calculator.
 * Module 17 Step 01.
 */
class PrintingCalculatorValidatorAndEngineTest {

    @Test
    fun testValidation_rejectsNegativeAndZeroQuantity() {
        val reqZero = PrintingCalculationRequest(
            tenantId = "T-01",
            projectId = "P-01",
            quantity = 0L,
            finishedWidth = BigDecimal("210.0000"),
            finishedHeight = BigDecimal("297.0000"),
            materialName = "Art Paper"
        )
        val res = PrintingCalculatorValidator.validateRequest(reqZero)
        assertFalse(res.isValid)
        assertTrue(res.diagnostics.any { it.code == DiagnosticCode.INVALID_QUANTITY })
    }

    @Test
    fun testValidation_rejectsSheetSmallerThanItem() {
        val reqSmallSheet = PrintingCalculationRequest(
            tenantId = "T-01",
            projectId = "P-01",
            quantity = 500L,
            finishedWidth = BigDecimal("300.0000"),
            finishedHeight = BigDecimal("400.0000"),
            materialName = "Art Paper",
            sheetWidth = BigDecimal("200.0000"),
            sheetHeight = BigDecimal("200.0000")
        )
        val res = PrintingCalculatorValidator.validateRequest(reqSmallSheet)
        assertFalse(res.isValid)
        assertTrue(res.diagnostics.any { it.code == DiagnosticCode.SHEET_SIZE_SMALLER_THAN_ITEM })
    }

    @Test
    fun testValidation_warnsOnExcessiveWaste() {
        val reqHighWaste = PrintingCalculationRequest(
            tenantId = "T-01",
            projectId = "P-01",
            quantity = 1000L,
            finishedWidth = BigDecimal("210.0000"),
            finishedHeight = BigDecimal("297.0000"),
            materialName = "Art Paper",
            runningWastePercentage = BigDecimal("35.0000")
        )
        val res = PrintingCalculatorValidator.validateRequest(reqHighWaste)
        assertTrue(res.isValid) // Warning is not error
        assertTrue(res.diagnostics.any { it.code == DiagnosticCode.EXCESSIVE_WASTE_PERCENTAGE && it.severity == DiagnosticSeverity.WARNING })
    }

    @Test
    fun testEngine_computesPhysicalAndCostBreakdownCorrectly() {
        val req = PrintingCalculationRequest(
            tenantId = "T-01",
            projectId = "P-01",
            jobTitle = "Corporate Brochure A4",
            productType = ProductType.PRINTING_JOB,
            quantity = 1000L,
            quantityUnit = QuantityUnit.PIECES,
            finishedWidth = BigDecimal("210.0000"),
            finishedHeight = BigDecimal("297.0000"),
            dimensionUnit = MeasurementUnit.MILLIMETERS,
            materialName = "Art Paper Gloss",
            stockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000"),
            sheetWidth = BigDecimal("635.0000"),
            sheetHeight = BigDecimal("914.0000"),
            sheetDimensionUnit = MeasurementUnit.MILLIMETERS,
            materialUnitPricePerSheet = BigDecimal("10.0000"),
            processType = PrintingProcessType.OFFSET,
            sides = PrintingSideOption.DOUBLE_SIDED_SAME,
            colorMode = ColorMode.CMYK_FOUR_COLOR,
            frontColorsCount = 4,
            backColorsCount = 4,
            setupSheets = 50L,
            runningWastePercentage = BigDecimal("5.0000"),
            finishingWastePercentage = BigDecimal("2.0000"),
            finishingOperations = listOf(
                FinishingOperationSpecification(
                    operationType = FinishingOperationType.GLOSS_LAMINATION,
                    unitRate = BigDecimal("1.5000"),
                    setupRate = BigDecimal("200.0000")
                )
            ),
            machine = MachineSpecification(
                machineName = "Heidelberg Speedmaster 4-Color",
                processType = PrintingProcessType.OFFSET,
                hourlyRate = BigDecimal("2500.0000"),
                impressionsPerHour = 5000,
                plateCostPerUnit = BigDecimal("400.0000")
            )
        )

        val spec = PrintingSpecificationNormalizer.normalize(req)
        val validation = PrintingCalculatorValidator.validateRequest(req)
        val result = PrintingCalculatorEngine.calculate(req, spec, validation.diagnostics)

        assertEquals(CalculationStatus.SUCCESSFUL, result.status)
        assertEquals(EstimateActualClassification.ESTIMATED, result.classification)
        assertEquals(9, result.materialRequirement.finishedItemsPerSheet)
        assertEquals(112L, result.materialRequirement.productiveSheetsRequired)
        assertEquals(59L, result.materialRequirement.wasteSheetsRequired)
        assertEquals(171L, result.materialRequirement.totalSheetsRequired)

        // Material Cost = 171 * 10 = 1710.0000
        assertEquals(BigDecimal("1710.0000"), result.materialRequirement.estimatedMaterialCost)

        // Plates = 8 plates @ 400 = 3200.0000
        assertEquals(8, result.printingRequirement.plateCount)
        assertEquals(BigDecimal("3200.0000"), result.printingRequirement.estimatedPlateCost)

        // Impressions = 171 * 2 sides = 342
        assertEquals(342L, result.printingRequirement.totalImpressions)

        // Finishing Lamination = (1000 * 1.50) + 200 = 1700.0000
        assertEquals(BigDecimal("1700.0000"), result.finishingRequirement.totalEstimatedFinishingCost)

        // Total Cost must be computed
        assertNotNull(result.totalEstimatedCost)
        assertTrue(result.totalEstimatedCost!! > BigDecimal("6000.0000"))
        assertNotNull(result.estimatedUnitCost)
        assertTrue(result.breakdownItems.isNotEmpty())
        assertTrue(result.integrityHash.isNotBlank())
    }

    @Test
    fun testEngine_partialCalculation_whenMaterialPriceMissing() {
        val reqNoPrice = PrintingCalculationRequest(
            tenantId = "T-01",
            projectId = "P-01",
            quantity = 1000L,
            finishedWidth = BigDecimal("210.0000"),
            finishedHeight = BigDecimal("297.0000"),
            materialName = "Art Paper",
            sheetWidth = BigDecimal("635.0000"),
            sheetHeight = BigDecimal("914.0000"),
            materialUnitPricePerSheet = null // Missing price
        )

        val spec = PrintingSpecificationNormalizer.normalize(reqNoPrice)
        val validation = PrintingCalculatorValidator.validateRequest(reqNoPrice)
        val result = PrintingCalculatorEngine.calculate(reqNoPrice, spec, validation.diagnostics)

        assertEquals(CalculationStatus.PARTIAL_CALCULATION, result.status)
        assertEquals(112L, result.materialRequirement.totalSheetsRequired)
        assertNull(result.materialRequirement.estimatedMaterialCost)
        assertNull(result.totalEstimatedCost)
        assertTrue(result.diagnostics.any { it.code == DiagnosticCode.MISSING_MATERIAL_PRICE })
    }
}

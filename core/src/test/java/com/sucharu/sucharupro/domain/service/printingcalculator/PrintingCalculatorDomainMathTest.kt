package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.printingcalculator.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Precision Math and Utility Tests for Smart Printing Calculator.
 * Module 17 Step 01.
 */
class PrintingCalculatorDomainMathTest {

    @Test
    fun testUnitConversionToMillimeters() {
        val mm = PrintingCalculatorMathUtils.toMillimeters(BigDecimal("100.0000"), MeasurementUnit.MILLIMETERS)
        assertEquals(BigDecimal("100.0000"), mm)

        val cmToMm = PrintingCalculatorMathUtils.toMillimeters(BigDecimal("10.0000"), MeasurementUnit.CENTIMETERS)
        assertEquals(BigDecimal("100.0000"), cmToMm)

        val inchesToMm = PrintingCalculatorMathUtils.toMillimeters(BigDecimal("10.0000"), MeasurementUnit.INCHES)
        assertEquals(BigDecimal("254.0000"), inchesToMm)

        val feetToMm = PrintingCalculatorMathUtils.toMillimeters(BigDecimal("1.0000"), MeasurementUnit.FEET)
        assertEquals(BigDecimal("304.8000"), feetToMm)
    }

    @Test
    fun testItemsPerSheet_orthogonalCalculation() {
        // Sheet: 635mm x 914mm (~ 25" x 36")
        // Item: 210mm x 297mm (A4)
        // Standard: 635/210 = 3, 914/297 = 3 -> 3 * 3 = 9 items per sheet
        // Rotated: 635/297 = 2, 914/210 = 4 -> 2 * 4 = 8 items per sheet
        // Selected: 9 items per sheet, STANDARD_PARALLEL
        val cut = PrintingCalculatorMathUtils.calculateItemsPerSheet(
            sheetWidthMm = BigDecimal("635.0000"),
            sheetHeightMm = BigDecimal("914.0000"),
            itemWidthMm = BigDecimal("210.0000"),
            itemHeightMm = BigDecimal("297.0000")
        )

        assertEquals(9, cut.itemsPerSheet)
        assertEquals("STANDARD_PARALLEL", cut.cutDirection)
        assertEquals(3, cut.cols)
        assertEquals(3, cut.rows)
        assertTrue(cut.wasteAreaPercentage > BigDecimal.ZERO)
        assertTrue(cut.wasteAreaPercentage < BigDecimal("50.0000"))
    }

    @Test
    fun testItemsPerSheet_rotatedOrientationSelected() {
        // Sheet: 500mm x 700mm
        // Item: 300mm x 200mm
        // Standard: 500/300 = 1, 700/200 = 3 -> 1 * 3 = 3 items
        // Rotated: 500/200 = 2, 700/300 = 2 -> 2 * 2 = 4 items
        // Selected: 4 items per sheet, ROTATED_90_DEG
        val cut = PrintingCalculatorMathUtils.calculateItemsPerSheet(
            sheetWidthMm = BigDecimal("500.0000"),
            sheetHeightMm = BigDecimal("700.0000"),
            itemWidthMm = BigDecimal("300.0000"),
            itemHeightMm = BigDecimal("200.0000")
        )

        assertEquals(4, cut.itemsPerSheet)
        assertEquals("ROTATED_90_DEG", cut.cutDirection)
    }

    @Test
    fun testItemsPerSheet_itemExceedsSheet() {
        val cut = PrintingCalculatorMathUtils.calculateItemsPerSheet(
            sheetWidthMm = BigDecimal("200.0000"),
            sheetHeightMm = BigDecimal("200.0000"),
            itemWidthMm = BigDecimal("300.0000"),
            itemHeightMm = BigDecimal("300.0000")
        )

        assertEquals(0, cut.itemsPerSheet)
        assertEquals("NONE_EXCEEDS_SHEET", cut.cutDirection)
    }

    @Test
    fun testProductiveSheets_andWasteCalculation() {
        // 1000 items, 9 items per sheet -> ceil(1000 / 9) = 112 productive sheets
        val productive = PrintingCalculatorMathUtils.calculateProductiveSheets(1000L, 9)
        assertEquals(112L, productive)

        // Setup = 50 sheets, Run waste = 5%, Finish waste = 2% -> Total waste % = 7%
        // Run waste = ceil(112 * 0.05) = 6 sheets
        // Finish waste = ceil(112 * 0.02) = 3 sheets
        // Total waste = 50 + 6 + 3 = 59 sheets
        val waste = PrintingCalculatorMathUtils.calculateTotalWasteSheets(
            productiveSheets = productive,
            setupSheets = 50L,
            runningWastePercentage = BigDecimal("5.0000"),
            finishingWastePercentage = BigDecimal("2.0000")
        )
        assertEquals(59L, waste)

        val totalSheets = productive + waste
        assertEquals(171L, totalSheets)

        // Reams: 171 / 500 = 0.3420 reams
        val reams = PrintingCalculatorMathUtils.calculateReams(totalSheets, 500)
        assertEquals(BigDecimal("0.3420"), reams)
    }

    @Test
    fun testPaperWeightKg_calculation() {
        // Sheet: 635mm x 914mm = 580,390 mm²
        // GSM: 150
        // Sheets: 1000
        // Total Grams = (635 * 914 * 150 * 1000) / 10^6 = 87,058.5 grams
        // Total Kg = 87.0585 kg
        val weight = PrintingCalculatorMathUtils.calculatePaperWeightKg(
            sheetWidthMm = BigDecimal("635.0000"),
            sheetHeightMm = BigDecimal("914.0000"),
            gsm = BigDecimal("150.0000"),
            totalSheets = 1000L
        )

        assertNotNull(weight)
        assertEquals(BigDecimal("87.0585"), weight)
    }

    @Test
    fun testPlateCount_calculation() {
        val colorOffsetDouble = ColorSpecification(
            colorMode = ColorMode.CMYK_FOUR_COLOR,
            frontColorsCount = 4,
            backColorsCount = 4,
            spotColorsCount = 0
        )
        val platesDouble = PrintingCalculatorMathUtils.calculatePlateCount(
            processType = PrintingProcessType.OFFSET,
            color = colorOffsetDouble,
            sides = PrintingSideOption.DOUBLE_SIDED_SAME
        )
        assertEquals(8, platesDouble)

        val colorOffsetSingle = ColorSpecification(
            colorMode = ColorMode.CMYK_PLUS_SPOT,
            frontColorsCount = 4,
            backColorsCount = 0,
            spotColorsCount = 1
        )
        val platesSingle = PrintingCalculatorMathUtils.calculatePlateCount(
            processType = PrintingProcessType.OFFSET,
            color = colorOffsetSingle,
            sides = PrintingSideOption.SINGLE_SIDED
        )
        assertEquals(5, platesSingle)

        val platesDigital = PrintingCalculatorMathUtils.calculatePlateCount(
            processType = PrintingProcessType.DIGITAL,
            color = colorOffsetDouble,
            sides = PrintingSideOption.DOUBLE_SIDED_SAME
        )
        assertEquals(0, platesDigital)
    }

    @Test
    fun testDeterministicFingerprinting_andIntegrityHash() {
        val fp1 = PrintingCalculatorMathUtils.generateRequestFingerprint(
            tenantId = "T-01",
            projectId = "P-01",
            quantity = 1000L,
            finishedWMm = BigDecimal("210.0000"),
            finishedHMm = BigDecimal("297.0000"),
            sheetWMm = BigDecimal("635.0000"),
            sheetHMm = BigDecimal("914.0000"),
            stockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000"),
            processType = PrintingProcessType.OFFSET,
            sides = PrintingSideOption.SINGLE_SIDED,
            colorMode = ColorMode.CMYK_FOUR_COLOR,
            frontColors = 4,
            backColors = 0,
            spotColors = 0,
            wastePct = BigDecimal("3.0000"),
            finishingOperations = emptyList()
        )

        val fp2 = PrintingCalculatorMathUtils.generateRequestFingerprint(
            tenantId = "T-01",
            projectId = "P-01",
            quantity = 1000L,
            finishedWMm = BigDecimal("210.0000"),
            finishedHMm = BigDecimal("297.0000"),
            sheetWMm = BigDecimal("635.0000"),
            sheetHMm = BigDecimal("914.0000"),
            stockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000"),
            processType = PrintingProcessType.OFFSET,
            sides = PrintingSideOption.SINGLE_SIDED,
            colorMode = ColorMode.CMYK_FOUR_COLOR,
            frontColors = 4,
            backColors = 0,
            spotColors = 0,
            wastePct = BigDecimal("3.0000"),
            finishingOperations = emptyList()
        )

        assertEquals(fp1, fp2)
        assertTrue(fp1.length == 64)

        val hash = PrintingCalculatorMathUtils.generateResultIntegrityHash(
            calculationId = "calc-01",
            tenantId = "T-01",
            projectId = "P-01",
            fingerprint = fp1,
            status = CalculationStatus.SUCCESSFUL,
            totalSheets = 171L,
            impressions = 171L,
            totalCost = BigDecimal("5000.0000"),
            unitCost = BigDecimal("5.0000"),
            calculatedAt = 1000000L
        )
        assertTrue(hash.isNotBlank())
        assertEquals(64, hash.length)
    }
}

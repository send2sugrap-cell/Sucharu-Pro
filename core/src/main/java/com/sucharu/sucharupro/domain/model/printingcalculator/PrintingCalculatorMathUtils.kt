package com.sucharu.sucharupro.domain.model.printingcalculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Precision Mathematical Utilities for the Smart Printing Calculator.
 * Scale = 4, RoundingMode.HALF_UP.
 * Module 17 Step 01.
 */
object PrintingCalculatorMathUtils {

    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    val ONE: BigDecimal = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP)
    val ONE_HUNDRED: BigDecimal = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP)
    val ONE_THOUSAND: BigDecimal = BigDecimal("1000.0000").setScale(4, RoundingMode.HALF_UP)
    val FIVE_HUNDRED: BigDecimal = BigDecimal("500.0000").setScale(4, RoundingMode.HALF_UP)
    private val AREA_WEIGHT_DIVISOR = BigDecimal("1000000000.0000") // 10^9 converts (mm * mm * gsm * sheets) -> kg

    fun scale4(value: BigDecimal?): BigDecimal {
        return value?.setScale(4, RoundingMode.HALF_UP) ?: ZERO
    }

    fun toMillimeters(value: BigDecimal, unit: MeasurementUnit): BigDecimal {
        return scale4(value.multiply(unit.toMmFactor))
    }

    fun fromMillimeters(valueMm: BigDecimal, targetUnit: MeasurementUnit): BigDecimal {
        if (targetUnit == MeasurementUnit.MILLIMETERS) return scale4(valueMm)
        return valueMm.divide(targetUnit.toMmFactor, 4, RoundingMode.HALF_UP)
    }

    data class SheetCutOrientation(
        val itemsPerSheet: Int,
        val cutDirection: String,
        val cols: Int,
        val rows: Int,
        val wasteAreaPercentage: BigDecimal
    )

    /**
     * Calculates basic orthogonal cuts per sheet and selects the optimal orientation.
     */
    fun calculateItemsPerSheet(
        sheetWidthMm: BigDecimal,
        sheetHeightMm: BigDecimal,
        itemWidthMm: BigDecimal,
        itemHeightMm: BigDecimal
    ): SheetCutOrientation {
        val sW = scale4(sheetWidthMm)
        val sH = scale4(sheetHeightMm)
        val iW = scale4(itemWidthMm)
        val iH = scale4(itemHeightMm)

        if (sW <= ZERO || sH <= ZERO || iW <= ZERO || iH <= ZERO) {
            return SheetCutOrientation(0, "INVALID", 0, 0, ONE_HUNDRED)
        }

        // 1. Orientation A: Standard (sheetW / itemW, sheetH / itemH)
        val colsA = sW.divide(iW, 0, RoundingMode.FLOOR).toInt()
        val rowsA = sH.divide(iH, 0, RoundingMode.FLOOR).toInt()
        val countA = colsA * rowsA

        // 2. Orientation B: Rotated (sheetW / itemH, sheetH / itemW)
        val colsB = sW.divide(iH, 0, RoundingMode.FLOOR).toInt()
        val rowsB = sH.divide(iW, 0, RoundingMode.FLOOR).toInt()
        val countB = colsB * rowsB

        val sheetArea = sW.multiply(sH)
        val itemArea = iW.multiply(iH)

        return if (countA >= countB && countA > 0) {
            val usedArea = itemArea.multiply(BigDecimal(countA))
            val wastePct = ONE_HUNDRED.subtract(
                usedArea.multiply(ONE_HUNDRED).divide(sheetArea, 4, RoundingMode.HALF_UP)
            ).coerceAtLeast(ZERO)
            SheetCutOrientation(countA, "STANDARD_PARALLEL", colsA, rowsA, wastePct)
        } else if (countB > 0) {
            val usedArea = itemArea.multiply(BigDecimal(countB))
            val wastePct = ONE_HUNDRED.subtract(
                usedArea.multiply(ONE_HUNDRED).divide(sheetArea, 4, RoundingMode.HALF_UP)
            ).coerceAtLeast(ZERO)
            SheetCutOrientation(countB, "ROTATED_90_DEG", colsB, rowsB, wastePct)
        } else {
            SheetCutOrientation(0, "NONE_EXCEEDS_SHEET", 0, 0, ONE_HUNDRED)
        }
    }

    /**
     * Calculates productive sheets required.
     */
    fun calculateProductiveSheets(normalizedQuantity: Long, itemsPerSheet: Int): Long {
        if (normalizedQuantity <= 0L || itemsPerSheet <= 0) return 0L
        val q = BigDecimal(normalizedQuantity)
        val ips = BigDecimal(itemsPerSheet)
        return q.divide(ips, 0, RoundingMode.CEILING).toLong()
    }

    /**
     * Calculates total waste sheets from setup, running waste %, and finishing waste %.
     */
    fun calculateTotalWasteSheets(
        productiveSheets: Long,
        setupSheets: Long,
        runningWastePercentage: BigDecimal,
        finishingWastePercentage: BigDecimal
    ): Long {
        val prod = BigDecimal(productiveSheets)
        val rPct = scale4(runningWastePercentage).coerceAtLeast(ZERO)
        val fPct = scale4(finishingWastePercentage).coerceAtLeast(ZERO)

        val runningWaste = prod.multiply(rPct).divide(ONE_HUNDRED, 0, RoundingMode.CEILING).toLong()
        val finishingWaste = prod.multiply(fPct).divide(ONE_HUNDRED, 0, RoundingMode.CEILING).toLong()
        val setup = setupSheets.coerceAtLeast(0L)

        return setup + runningWaste + finishingWaste
    }

    /**
     * Calculates ream requirement (standard 500 sheets per ream).
     */
    fun calculateReams(totalSheets: Long, sheetsPerReam: Int = 500): BigDecimal {
        if (totalSheets <= 0L || sheetsPerReam <= 0) return ZERO
        val sheets = BigDecimal(totalSheets)
        val reamSize = BigDecimal(sheetsPerReam)
        return sheets.divide(reamSize, 4, RoundingMode.HALF_UP)
    }

    /**
     * Calculates paper substrate weight in kilograms:
     * (widthMm * heightMm * gsm * sheets) / 10^9.
     */
    fun calculatePaperWeightKg(
        sheetWidthMm: BigDecimal,
        sheetHeightMm: BigDecimal,
        gsm: BigDecimal?,
        totalSheets: Long
    ): BigDecimal? {
        if (gsm == null || gsm <= ZERO || totalSheets <= 0L) return null
        val w = scale4(sheetWidthMm)
        val h = scale4(sheetHeightMm)
        val g = scale4(gsm)
        val s = BigDecimal(totalSheets)

        val totalGrams = w.multiply(h).multiply(g).multiply(s).divide(BigDecimal("1000000.0000"), 4, RoundingMode.HALF_UP)
        return totalGrams.divide(ONE_THOUSAND, 4, RoundingMode.HALF_UP)
    }

    /**
     * Calculates plate requirement for Offset and Flexographic processes.
     */
    fun calculatePlateCount(
        processType: PrintingProcessType,
        color: ColorSpecification,
        sides: PrintingSideOption
    ): Int {
        if (!processType.allowsPlates) return 0

        val frontPlates = color.frontColorsCount + color.spotColorsCount
        val backPlates = if (sides.isDoubleSided) color.backColorsCount + color.spotColorsCount else 0
        return frontPlates + backPlates
    }

    /**
     * Calculates printing impressions.
     */
    fun calculateImpressions(
        totalSheets: Long,
        sides: PrintingSideOption
    ): Long {
        if (totalSheets <= 0L) return 0L
        return totalSheets * sides.sideCount
    }

    /**
     * Calculates unit cost safely avoiding division by zero.
     */
    fun calculateUnitCost(totalCost: BigDecimal?, quantity: Long): BigDecimal? {
        if (totalCost == null || quantity <= 0L) return null
        return totalCost.divide(BigDecimal(quantity), 4, RoundingMode.HALF_UP)
    }

    /**
     * SHA-256 calculation.
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a deterministic request fingerprint.
     */
    fun generateRequestFingerprint(
        tenantId: String,
        projectId: String,
        quantity: Long,
        finishedWMm: BigDecimal,
        finishedHMm: BigDecimal,
        sheetWMm: BigDecimal?,
        sheetHMm: BigDecimal?,
        stockType: PaperStockType,
        gsm: BigDecimal?,
        processType: PrintingProcessType,
        sides: PrintingSideOption,
        colorMode: ColorMode,
        frontColors: Int,
        backColors: Int,
        spotColors: Int,
        wastePct: BigDecimal,
        finishingOperations: List<FinishingOperationSpecification>
    ): String {
        val finishingSignature = finishingOperations
            .sortedBy { it.operationType.name }
            .joinToString("|") { "${it.operationType.name}:${scale4(it.unitRate)}:${scale4(it.setupRate)}" }

        val raw = buildString {
            append("CALC-FP:")
            append(tenantId).append(":")
            append(projectId).append(":")
            append(quantity).append(":")
            append(scale4(finishedWMm)).append("x").append(scale4(finishedHMm)).append(":")
            append(sheetWMm?.let { scale4(it) } ?: "AUTO").append("x").append(sheetHMm?.let { scale4(it) } ?: "AUTO").append(":")
            append(stockType.name).append(":")
            append(gsm?.let { scale4(it) } ?: "NONE").append(":")
            append(processType.name).append(":")
            append(sides.name).append(":")
            append(colorMode.name).append(":")
            append(frontColors).append("/").append(backColors).append("+").append(spotColors).append(":")
            append(scale4(wastePct)).append(":")
            append(finishingSignature)
        }
        return sha256(raw)
    }

    /**
     * Generates an immutable calculation result integrity hash.
     */
    fun generateResultIntegrityHash(
        calculationId: String,
        tenantId: String,
        projectId: String,
        fingerprint: String,
        status: CalculationStatus,
        totalSheets: Long,
        impressions: Long,
        totalCost: BigDecimal?,
        unitCost: BigDecimal?,
        calculatedAt: Long
    ): String {
        val raw = buildString {
            append("CALC-RESULT-HASH:")
            append(calculationId).append(":")
            append(tenantId).append(":")
            append(projectId).append(":")
            append(fingerprint).append(":")
            append(status.name).append(":")
            append(totalSheets).append(":")
            append(impressions).append(":")
            append(totalCost?.let { scale4(it) } ?: "UNAVAILABLE").append(":")
            append(unitCost?.let { scale4(it) } ?: "UNAVAILABLE").append(":")
            append(calculatedAt)
        }
        return sha256(raw)
    }
}

package com.sucharu.sucharupro.domain.model.imposition

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Pure Precision Mathematical Utilities for Imposition & Sheet Layout Calculations.
 * Scale = 4, RoundingMode.HALF_UP.
 * Module 18 Step 01.
 */
object ImpositionMathUtils {

    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    val ONE: BigDecimal = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP)
    val ONE_HUNDRED: BigDecimal = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP)

    fun scale4(value: BigDecimal?): BigDecimal {
        return value?.setScale(4, RoundingMode.HALF_UP) ?: ZERO
    }

    /**
     * Converts any dimension to canonical Millimeters using pure BigDecimal arithmetic.
     */
    fun toMillimeters(dimension: PrintingDimension): PrintingDimension {
        if (dimension.unit == MeasurementUnit.MILLIMETERS) {
            return PrintingDimension(
                scale4(dimension.width),
                scale4(dimension.height),
                MeasurementUnit.MILLIMETERS
            )
        }
        val wMm = scale4(dimension.width.multiply(dimension.unit.toMmFactor))
        val hMm = scale4(dimension.height.multiply(dimension.unit.toMmFactor))
        return PrintingDimension(wMm, hMm, MeasurementUnit.MILLIMETERS)
    }

    /**
     * Calculates usable sheet dimensions after deducting outer margins.
     */
    fun calculateUsableSheetDimension(
        sheetWidthMm: BigDecimal,
        sheetHeightMm: BigDecimal,
        margins: ImpositionMarginSpec
    ): Pair<BigDecimal, BigDecimal> {
        val sW = scale4(sheetWidthMm)
        val sH = scale4(sheetHeightMm)
        val usableW = sW.subtract(margins.totalHorizontalMarginMm).coerceAtLeast(ZERO)
        val usableH = sH.subtract(margins.totalVerticalMarginMm).coerceAtLeast(ZERO)
        return Pair(usableW, usableH)
    }

    /**
     * Evaluates candidate layout for a specific orientation.
     *
     * Formula:
     * When finished items are placed with a gutter between them:
     * Available span for N items = N * itemDim + (N - 1) * gutter
     * (usableSpan + gutter) / (itemDim + gutter) >= N
     * N = floor((usableSpan + gutter) / (itemDim + gutter))
     */
    fun evaluateCandidate(
        orientation: ImpositionLayoutOrientation,
        usableWidthMm: BigDecimal,
        usableHeightMm: BigDecimal,
        itemWidthMm: BigDecimal,
        itemHeightMm: BigDecimal,
        spacing: ImpositionSpacingSpec
    ): ImpositionCandidate {
        val uW = scale4(usableWidthMm)
        val uH = scale4(usableHeightMm)
        val iW = scale4(itemWidthMm)
        val iH = scale4(itemHeightMm)
        val gH = scale4(spacing.horizontalGutterMm).coerceAtLeast(ZERO)
        val gV = scale4(spacing.verticalGutterMm).coerceAtLeast(ZERO)

        if (uW <= ZERO || uH <= ZERO || iW <= ZERO || iH <= ZERO) {
            return ImpositionCandidate(
                orientation = orientation,
                columns = 0,
                rows = 0,
                copiesPerSheet = 0,
                usableWidthMm = uW,
                usableHeightMm = uH,
                itemEffectiveWidthMm = iW,
                itemEffectiveHeightMm = iH,
                occupiedAreaMm2 = ZERO,
                usableAreaMm2 = uW.multiply(uH),
                wasteAreaMm2 = uW.multiply(uH),
                yieldPercentage = ZERO,
                isFeasible = false
            )
        }

        // Determine effective dimension based on orientation
        val (effItemW, effItemH) = when (orientation) {
            ImpositionLayoutOrientation.STANDARD -> Pair(iW, iH)
            ImpositionLayoutOrientation.ROTATED -> Pair(iH, iW)
        }

        // Check feasibility of fitting at least one item
        if (effItemW > uW || effItemH > uH) {
            return ImpositionCandidate(
                orientation = orientation,
                columns = 0,
                rows = 0,
                copiesPerSheet = 0,
                usableWidthMm = uW,
                usableHeightMm = uH,
                itemEffectiveWidthMm = effItemW,
                itemEffectiveHeightMm = effItemH,
                occupiedAreaMm2 = ZERO,
                usableAreaMm2 = uW.multiply(uH),
                wasteAreaMm2 = uW.multiply(uH),
                yieldPercentage = ZERO,
                isFeasible = false
            )
        }

        val cols = uW.add(gH).divide(effItemW.add(gH), 0, RoundingMode.FLOOR).toInt().coerceAtLeast(0)
        val rows = uH.add(gV).divide(effItemH.add(gV), 0, RoundingMode.FLOOR).toInt().coerceAtLeast(0)
        val copies = cols * rows

        val usableArea = uW.multiply(uH)
        val singleItemArea = iW.multiply(iH)
        val occupiedArea = singleItemArea.multiply(BigDecimal(copies))
        val wasteArea = usableArea.subtract(occupiedArea).coerceAtLeast(ZERO)

        val yieldPct = if (usableArea > ZERO && copies > 0) {
            occupiedArea.multiply(ONE_HUNDRED).divide(usableArea, 4, RoundingMode.HALF_UP).coerceIn(ZERO, ONE_HUNDRED)
        } else {
            ZERO
        }

        return ImpositionCandidate(
            orientation = orientation,
            columns = cols,
            rows = rows,
            copiesPerSheet = copies,
            usableWidthMm = uW,
            usableHeightMm = uH,
            itemEffectiveWidthMm = effItemW,
            itemEffectiveHeightMm = effItemH,
            occupiedAreaMm2 = occupiedArea,
            usableAreaMm2 = usableArea,
            wasteAreaMm2 = wasteArea,
            yieldPercentage = yieldPct,
            isFeasible = copies > 0
        )
    }

    /**
     * Calculates required sheet count given quantity and copies per sheet.
     * requiredSheets = ceil(requiredQuantity / copiesPerSheet)
     */
    fun calculateRequiredSheets(requiredQuantity: Long, copiesPerSheet: Int): Long {
        if (requiredQuantity <= 0L || copiesPerSheet <= 0) return 0L
        val q = BigDecimal(requiredQuantity)
        val cps = BigDecimal(copiesPerSheet)
        return q.divide(cps, 0, RoundingMode.CEILING).toLong()
    }

    /**
     * Calculates total produced capacity and layout overage items.
     */
    fun calculateOverage(requiredQuantity: Long, requiredSheets: Long, copiesPerSheet: Int): Pair<Long, Long> {
        val totalCapacity = requiredSheets * copiesPerSheet.toLong()
        val overage = (totalCapacity - requiredQuantity).coerceAtLeast(0L)
        return Pair(totalCapacity, overage)
    }

    /**
     * Generates a deterministic SHA-256 integrity hash for an imposition layout.
     */
    fun generateImpositionIntegrityHash(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        sheetW: BigDecimal,
        sheetH: BigDecimal,
        itemW: BigDecimal,
        itemH: BigDecimal,
        orientation: String,
        copiesPerSheet: Int,
        requiredSheets: Long
    ): String {
        val payload = "$tenantId:$orderId:$orderItemId:$sheetW:$sheetH:$itemW:$itemH:$orientation:$copiesPerSheet:$requiredSheets"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

object SubstrateReservationMathUtils {

    private val MM_TO_METERS = BigDecimal("0.0010")
    private val REAM_SHEET_COUNT = BigDecimal("500.0000")
    private val GRAMS_TO_KG = BigDecimal("0.0010")

    /**
     * Converts sheet dimensions to millimeters with strict 4 decimal scale.
     */
    fun toMillimeters(dimension: PrintingDimension): PrintingDimension {
        val widthMm = dimension.width.multiply(dimension.unit.toMmFactor).setScale(4, RoundingMode.HALF_UP)
        val heightMm = dimension.height.multiply(dimension.unit.toMmFactor).setScale(4, RoundingMode.HALF_UP)
        return PrintingDimension(widthMm, heightMm, MeasurementUnit.MILLIMETERS)
    }

    /**
     * Calculates the surface area of a single parent sheet in square meters ($m^2$).
     */
    fun calculateSheetAreaSqM(dimension: PrintingDimension): BigDecimal {
        val dimMm = toMillimeters(dimension)
        val widthMeters = dimMm.width.multiply(MM_TO_METERS).setScale(6, RoundingMode.HALF_UP)
        val heightMeters = dimMm.height.multiply(MM_TO_METERS).setScale(6, RoundingMode.HALF_UP)
        return widthMeters.multiply(heightMeters).setScale(6, RoundingMode.HALF_UP)
    }

    /**
     * Computes the total ream quantity for a given gross sheet count.
     * 1 Ream = 500 Sheets.
     */
    fun calculateReams(sheets: Long): BigDecimal {
        if (sheets <= 0L) return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        return BigDecimal(sheets).divide(REAM_SHEET_COUNT, 4, RoundingMode.HALF_UP)
    }

    /**
     * Calculates total physical weight of substrate stock in kilograms (kg).
     * Formula: $\text{Weight (kg)} = \text{Sheets} \times \text{Area } (m^2) \times \frac{\text{GSM}}{1000}$
     */
    fun calculateTotalWeightKg(sheets: Long, gsm: BigDecimal, dimension: PrintingDimension): BigDecimal {
        if (sheets <= 0L || gsm <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val areaSqM = calculateSheetAreaSqM(dimension)
        val weightPerSheetKg = areaSqM.multiply(gsm).multiply(GRAMS_TO_KG).setScale(6, RoundingMode.HALF_UP)
        return BigDecimal(sheets).multiply(weightPerSheetKg).setScale(4, RoundingMode.HALF_UP)
    }

    /**
     * Evaluates reservable available stock strictly as:
     * $\text{Available} = \max(0, \text{OnHand} - \text{ActiveReservations})$
     */
    fun calculateAvailableStock(onHandPhysical: Long, totalActiveReserved: Long): Long {
        val diff = onHandPhysical - totalActiveReserved
        return if (diff < 0L) 0L else diff
    }

    /**
     * Generates a deterministic SHA-256 fingerprint for idempotency & audit sealing.
     */
    fun generateDeterministicReservationNonce(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        sku: String
    ): String {
        val raw = "$tenantId:$orderId:$orderItemId:${sku.trim().uppercase()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

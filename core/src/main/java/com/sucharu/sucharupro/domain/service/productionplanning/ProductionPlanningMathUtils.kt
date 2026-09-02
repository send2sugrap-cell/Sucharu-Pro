package com.sucharu.sucharupro.domain.service.productionplanning

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

object ProductionPlanningMathUtils {

    val SCALE = 4
    val ROUNDING = RoundingMode.HALF_UP
    val ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING)
    val ONE_HUNDRED = BigDecimal("100.0000")

    // Weighted scoring constants (sums to 1.0000)
    val WEIGHT_COMMERCIAL = BigDecimal("0.2000")
    val WEIGHT_SPECIFICATION = BigDecimal("0.3000")
    val WEIGHT_MATERIAL = BigDecimal("0.2000")
    val WEIGHT_MACHINE = BigDecimal("0.1500")
    val WEIGHT_SCHEDULE = BigDecimal("0.1500")

    fun round(value: BigDecimal): BigDecimal {
        return value.setScale(SCALE, ROUNDING)
    }

    /**
     * Computes deterministic planned production quantity.
     * Planned Quantity = Ordered Quantity + Setup/MakeReady + Waste Quantity
     */
    fun calculatePlannedQuantity(
        orderedQuantity: Long,
        makeReadySheets: Long,
        wastePercentage: BigDecimal
    ): Long {
        val base = BigDecimal.valueOf(orderedQuantity)
        val wasteQty = base.multiply(wastePercentage.divide(ONE_HUNDRED, SCALE, ROUNDING)).setScale(0, RoundingMode.CEILING).toLong()
        return orderedQuantity + makeReadySheets + wasteQty
    }

    /**
     * Computes the weighted Manufacturing Readiness Score (0.0000 to 100.0000).
     * If there are critical blocking issues, the score is capped to 49.0000 to reflect NOT READY.
     */
    fun calculateReadinessScore(
        commercialScore: BigDecimal,
        specificationScore: BigDecimal,
        materialScore: BigDecimal,
        machineScore: BigDecimal,
        scheduleScore: BigDecimal,
        hasCriticalBlockers: Boolean
    ): BigDecimal {
        val weightedSum = commercialScore.multiply(WEIGHT_COMMERCIAL)
            .add(specificationScore.multiply(WEIGHT_SPECIFICATION))
            .add(materialScore.multiply(WEIGHT_MATERIAL))
            .add(machineScore.multiply(WEIGHT_MACHINE))
            .add(scheduleScore.multiply(WEIGHT_SCHEDULE))
            .setScale(SCALE, ROUNDING)

        val clamped = weightedSum.coerceIn(ZERO, ONE_HUNDRED)
        return if (hasCriticalBlockers && clamped >= BigDecimal("50.0000")) {
            BigDecimal("49.0000")
        } else {
            clamped
        }
    }

    /**
     * Deterministic SHA-256 fingerprint generator.
     */
    fun generateFingerprint(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        specFingerprint: String,
        orderedQuantity: Long,
        plannedQuantity: Long,
        status: String
    ): String {
        val raw = "$tenantId|$orderId|$orderItemId|$specFingerprint|$orderedQuantity|$plannedQuantity|$status"
        return sha256(raw)
    }

    /**
     * SHA-256 hash generator.
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

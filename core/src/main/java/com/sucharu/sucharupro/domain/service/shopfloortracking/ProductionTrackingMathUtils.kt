package com.sucharu.sucharupro.domain.service.shopfloortracking

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

object ProductionTrackingMathUtils {

    const val SCALE = 4
    val ROUNDING_MODE = RoundingMode.HALF_UP

    fun scale(value: BigDecimal): BigDecimal = value.setScale(SCALE, ROUNDING_MODE)

    fun scale(value: Double): BigDecimal = BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE)

    fun scale(value: Long): BigDecimal = BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE)

    fun scale(value: Int): BigDecimal = BigDecimal.valueOf(value.toLong()).setScale(SCALE, ROUNDING_MODE)

    fun calculateYieldPercentage(goodQuantity: BigDecimal, totalInputQuantity: BigDecimal): BigDecimal {
        if (totalInputQuantity.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal("100.0000")
        return goodQuantity
            .multiply(BigDecimal("100.0000"))
            .divide(totalInputQuantity, SCALE, ROUNDING_MODE)
    }

    fun calculateEfficiencyRatio(standardDurationMinutes: Int, actualDurationMinutes: Int): BigDecimal {
        if (actualDurationMinutes <= 0) return BigDecimal("1.0000")
        return scale(standardDurationMinutes)
            .divide(scale(actualDurationMinutes), SCALE, ROUNDING_MODE)
    }

    fun calculateSpeedEfficiency(actualSpeedUnitsPerHour: BigDecimal, ratedSpeedUnitsPerHour: BigDecimal): BigDecimal {
        if (ratedSpeedUnitsPerHour.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal("100.0000")
        return actualSpeedUnitsPerHour
            .multiply(BigDecimal("100.0000"))
            .divide(ratedSpeedUnitsPerHour, SCALE, ROUNDING_MODE)
    }

    fun calculateMaterialVariancePercentage(actualQuantity: BigDecimal, plannedQuantity: BigDecimal): BigDecimal {
        if (plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO
        val diff = actualQuantity.subtract(plannedQuantity)
        return diff
            .multiply(BigDecimal("100.0000"))
            .divide(plannedQuantity, SCALE, ROUNDING_MODE)
    }

    fun sha256Hex(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

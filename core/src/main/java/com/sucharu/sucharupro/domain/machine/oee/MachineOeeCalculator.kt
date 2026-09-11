package com.sucharu.sucharupro.domain.machine.oee

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max

/**
 * Pure deterministic calculation engine for Machine OEE and performance metrics.
 * Formula: OEE = Availability × Performance × Quality
 */
object MachineOeeCalculator {

    data class OeeCalculationResult(
        val runTimeSeconds: Long,
        val availabilityRatio: BigDecimal,
        val performanceRatio: BigDecimal,
        val qualityRatio: BigDecimal,
        val oeeRatio: BigDecimal
    )

    fun calculate(
        plannedProductionSeconds: Long,
        downtimeSeconds: Long,
        idealRateUnitsPerHour: BigDecimal,
        actualOutputUnits: BigDecimal,
        goodOutputUnits: BigDecimal,
        rejectedOutputUnits: BigDecimal
    ): OeeCalculationResult {
        // 1. Run Time & Availability
        val safePlannedSeconds = max(0L, plannedProductionSeconds)
        val safeDowntimeSeconds = max(0L, downtimeSeconds)
        val runTimeSeconds = max(0L, safePlannedSeconds - safeDowntimeSeconds)

        val availabilityRatio = if (safePlannedSeconds > 0L) {
            val ratio = BigDecimal(runTimeSeconds).divide(BigDecimal(safePlannedSeconds), 6, RoundingMode.HALF_UP)
            ratio.coerceAtMost(BigDecimal.ONE)
        } else {
            BigDecimal.ZERO
        }

        // 2. Quality = Good Output / Total Output
        val totalOutput = goodOutputUnits.add(rejectedOutputUnits).max(actualOutputUnits)
        val qualityRatio = if (totalOutput > BigDecimal.ZERO) {
            val ratio = goodOutputUnits.divide(totalOutput, 6, RoundingMode.HALF_UP)
            ratio.coerceAtMost(BigDecimal.ONE)
        } else {
            if (runTimeSeconds > 0L) BigDecimal.ONE else BigDecimal.ZERO
        }

        // 3. Performance = Actual Output / Theoretical Maximum Output
        // Theoretical Max Output = Ideal Rate Per Hour * (Run Time Seconds / 3600)
        val performanceRatio = if (runTimeSeconds > 0L && idealRateUnitsPerHour > BigDecimal.ZERO) {
            val runTimeHours = BigDecimal(runTimeSeconds).divide(BigDecimal("3600"), 6, RoundingMode.HALF_UP)
            val theoreticalMaxOutput = idealRateUnitsPerHour.multiply(runTimeHours)
            if (theoreticalMaxOutput > BigDecimal.ZERO) {
                actualOutputUnits.divide(theoreticalMaxOutput, 6, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
        } else {
            BigDecimal.ZERO
        }

        // 4. OEE = Availability × Performance × Quality
        val oeeRatio = availabilityRatio
            .multiply(performanceRatio)
            .multiply(qualityRatio)
            .setScale(4, RoundingMode.HALF_UP)

        return OeeCalculationResult(
            runTimeSeconds = runTimeSeconds,
            availabilityRatio = availabilityRatio.setScale(4, RoundingMode.HALF_UP),
            performanceRatio = performanceRatio.setScale(4, RoundingMode.HALF_UP),
            qualityRatio = qualityRatio.setScale(4, RoundingMode.HALF_UP),
            oeeRatio = oeeRatio
        )
    }
}

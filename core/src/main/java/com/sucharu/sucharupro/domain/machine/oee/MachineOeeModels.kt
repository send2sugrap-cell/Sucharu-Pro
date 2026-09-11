package com.sucharu.sucharupro.domain.machine.oee

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Machine OEE Metric Summary domain model.
 * OEE = Availability × Performance × Quality
 */
data class MachineOeeMetrics(
    val metricId: String,
    val tenantId: String,
    val machineId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val plannedProductionSeconds: Long,
    val runTimeSeconds: Long,
    val downtimeSeconds: Long,
    val idealRateUnitsPerHour: BigDecimal,
    val actualOutputUnits: BigDecimal,
    val goodOutputUnits: BigDecimal,
    val rejectedOutputUnits: BigDecimal,
    val availabilityRatio: BigDecimal,
    val performanceRatio: BigDecimal,
    val qualityRatio: BigDecimal,
    val oeeRatio: BigDecimal,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null
) {
    val availabilityPercentage: BigDecimal
        get() = (availabilityRatio * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)

    val performancePercentage: BigDecimal
        get() = (performanceRatio * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)

    val qualityPercentage: BigDecimal
        get() = (qualityRatio * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)

    val oeePercentage: BigDecimal
        get() = (oeeRatio * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
}

/**
 * Request parameter model for OEE calculation over a specific period.
 */
data class MachineOeeCalculationRequest(
    val tenantId: String,
    val machineId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val customPlannedProductionSeconds: Long? = null,
    val customIdealRateUnitsPerHour: BigDecimal? = null
)

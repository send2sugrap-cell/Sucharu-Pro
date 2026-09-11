package com.sucharu.sucharupro.data.api.model.machine.oee

import com.sucharu.sucharupro.domain.machine.oee.MachineOeeMetrics
import java.math.BigDecimal

/**
 * REST API DTOs for Machine Performance & OEE calculations.
 */
data class CalculateOeeRequestDto(
    val periodStart: Long,
    val periodEnd: Long,
    val customPlannedProductionSeconds: Long? = null,
    val customIdealRateUnitsPerHour: BigDecimal? = null
)

data class MachineOeeResponseDto(
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
    val availabilityPercentage: BigDecimal,
    val performancePercentage: BigDecimal,
    val qualityPercentage: BigDecimal,
    val oeePercentage: BigDecimal,
    val createdAt: Long,
    val createdBy: String?
) {
    companion object {
        fun fromDomain(domain: MachineOeeMetrics): MachineOeeResponseDto = MachineOeeResponseDto(
            metricId = domain.metricId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            periodStart = domain.periodStart,
            periodEnd = domain.periodEnd,
            plannedProductionSeconds = domain.plannedProductionSeconds,
            runTimeSeconds = domain.runTimeSeconds,
            downtimeSeconds = domain.downtimeSeconds,
            idealRateUnitsPerHour = domain.idealRateUnitsPerHour,
            actualOutputUnits = domain.actualOutputUnits,
            goodOutputUnits = domain.goodOutputUnits,
            rejectedOutputUnits = domain.rejectedOutputUnits,
            availabilityRatio = domain.availabilityRatio,
            performanceRatio = domain.performanceRatio,
            qualityRatio = domain.qualityRatio,
            oeeRatio = domain.oeeRatio,
            availabilityPercentage = domain.availabilityPercentage,
            performancePercentage = domain.performancePercentage,
            qualityPercentage = domain.qualityPercentage,
            oeePercentage = domain.oeePercentage,
            createdAt = domain.createdAt,
            createdBy = domain.createdBy
        )
    }
}

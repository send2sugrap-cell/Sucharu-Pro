package com.sucharu.sucharupro.data.api.model.machine.health

import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.health.MachineHealthCondition
import com.sucharu.sucharupro.domain.machine.health.MachineHealthSnapshot
import com.sucharu.sucharupro.domain.machine.health.MachineOperationalState
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import java.math.BigDecimal

/**
 * REST API DTOs for Machine Operational Status & Health Monitoring.
 */
data class MachineHealthResponseDto(
    val machineId: String,
    val tenantId: String,
    val assetCode: String,
    val machineName: String,
    val machineType: MachineType,
    val masterStatus: MachineStatus,
    val operationalState: MachineOperationalState,
    val healthCondition: MachineHealthCondition,
    val lastEventTimestamp: Long?,
    val lastIngestedAt: Long?,
    val telemetryFreshnessMs: Long?,
    val isFresh: Boolean,
    val latestMetricValue: BigDecimal?,
    val latestMetricType: TelemetryMetricType?,
    val activeMessage: String?,
    val evaluatedAt: Long
) {
    companion object {
        fun fromDomain(domain: MachineHealthSnapshot): MachineHealthResponseDto = MachineHealthResponseDto(
            machineId = domain.machineId,
            tenantId = domain.tenantId,
            assetCode = domain.assetCode,
            machineName = domain.machineName,
            machineType = domain.machineType,
            masterStatus = domain.masterStatus,
            operationalState = domain.operationalState,
            healthCondition = domain.healthCondition,
            lastEventTimestamp = domain.lastEventTimestamp,
            lastIngestedAt = domain.lastIngestedAt,
            telemetryFreshnessMs = domain.telemetryFreshnessMs,
            isFresh = domain.isFresh,
            latestMetricValue = domain.latestMetricValue,
            latestMetricType = domain.latestMetricType,
            activeMessage = domain.activeMessage,
            evaluatedAt = domain.evaluatedAt
        )
    }
}

data class MachineHealthListResponseDto(
    val machines: List<MachineHealthResponseDto>,
    val totalCount: Int = machines.size
)

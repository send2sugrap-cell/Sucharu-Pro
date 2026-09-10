package com.sucharu.sucharupro.domain.machine.health

import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import java.math.BigDecimal

/**
 * Canonical operational states for production machinery.
 */
enum class MachineOperationalState {
    RUNNING,
    IDLE,
    OFFLINE,
    WARNING,
    FAULT,
    UNKNOWN
}

/**
 * Basic health condition classification.
 */
enum class MachineHealthCondition {
    HEALTHY,
    DEGRADED,
    CRITICAL,
    UNKNOWN
}

/**
 * Configurable policies and thresholds for machine health evaluation.
 */
data class MachineHealthPolicy(
    val freshnessThresholdMs: Long = 300000L, // 5 minutes default
    val maxFutureAllowanceMs: Long = 86400000L // 24 hours future allowance
)

/**
 * Snapshot of machine operational status and health condition.
 */
data class MachineHealthSnapshot(
    val machineId: String,
    val tenantId: String,
    val assetCode: String,
    val machineName: String,
    val machineType: MachineType,
    val masterStatus: MachineStatus,
    val operationalState: MachineOperationalState,
    val healthCondition: MachineHealthCondition,
    val lastEventTimestamp: Long? = null,
    val lastIngestedAt: Long? = null,
    val telemetryFreshnessMs: Long? = null,
    val isFresh: Boolean = false,
    val latestMetricValue: BigDecimal? = null,
    val latestMetricType: TelemetryMetricType? = null,
    val activeMessage: String? = null,
    val evaluatedAt: Long = System.currentTimeMillis()
)

package com.sucharu.sucharupro.domain.machine.alerts

import com.sucharu.sucharupro.domain.machine.events.FaultSeverity
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import java.math.BigDecimal

/**
 * Types of machine operational alerts.
 */
enum class MachineAlertType {
    TELEMETRY_ABNORMAL,
    HEALTH_CRITICAL,
    FAULT_EVENT,
    MAINTENANCE_DUE,
    MAINTENANCE_OVERDUE,
    WARNING
}

/**
 * Lifecycle status of machine operational alerts.
 */
enum class MachineAlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED,
    DISMISSED;

    val isTerminal: Boolean get() = this == RESOLVED || this == DISMISSED
}

/**
 * Canonical Machine Operational Alert domain entity.
 */
data class MachineOperationalAlert(
    val alertId: String,
    val tenantId: String,
    val machineId: String,
    val alertType: MachineAlertType,
    val severity: FaultSeverity = FaultSeverity.WARNING,
    val status: MachineAlertStatus = MachineAlertStatus.ACTIVE,
    val source: String = "SYSTEM",
    val telemetryRecordId: String? = null,
    val faultEventId: String? = null,
    val maintenanceScheduleId: String? = null,
    val maintenanceRecordId: String? = null,
    val title: String,
    val description: String,
    val correlationKey: String? = null,
    val notificationId: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null
)

/**
 * Telemetry alert evaluation threshold rule.
 */
data class TelemetryAlertRule(
    val ruleId: String,
    val tenantId: String,
    val machineId: String? = null,
    val metricType: TelemetryMetricType,
    val operator: String, // GREATER_THAN, LESS_THAN, EQUALS
    val thresholdValue: BigDecimal,
    val severity: FaultSeverity = FaultSeverity.WARNING,
    val isEnabled: Boolean = true
)

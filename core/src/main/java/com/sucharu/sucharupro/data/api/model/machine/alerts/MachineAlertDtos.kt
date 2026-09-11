package com.sucharu.sucharupro.data.api.model.machine.alerts

import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertType
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.machine.events.FaultSeverity

/**
 * REST API DTOs for Machine Operational Alerts & Notifications.
 */
data class CreateMachineAlertRequestDto(
    val alertType: MachineAlertType = MachineAlertType.WARNING,
    val severity: FaultSeverity = FaultSeverity.WARNING,
    val source: String = "MANUAL",
    val telemetryRecordId: String? = null,
    val faultEventId: String? = null,
    val maintenanceScheduleId: String? = null,
    val maintenanceRecordId: String? = null,
    val title: String,
    val description: String,
    val correlationKey: String? = null
)

data class ResolveMachineAlertRequestDto(
    val resolutionNotes: String? = null
)

data class DismissMachineAlertRequestDto(
    val reason: String? = null
)

data class MachineOperationalAlertResponseDto(
    val alertId: String,
    val tenantId: String,
    val machineId: String,
    val alertType: MachineAlertType,
    val severity: FaultSeverity,
    val status: MachineAlertStatus,
    val source: String,
    val telemetryRecordId: String?,
    val faultEventId: String?,
    val maintenanceScheduleId: String?,
    val maintenanceRecordId: String?,
    val title: String,
    val description: String,
    val correlationKey: String?,
    val notificationId: String?,
    val occurredAt: Long,
    val acknowledgedAt: Long?,
    val acknowledgedBy: String?,
    val resolvedAt: Long?,
    val resolvedBy: String?,
    val resolutionNotes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String?,
    val updatedBy: String?
) {
    companion object {
        fun fromDomain(domain: MachineOperationalAlert): MachineOperationalAlertResponseDto = MachineOperationalAlertResponseDto(
            alertId = domain.alertId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            alertType = domain.alertType,
            severity = domain.severity,
            status = domain.status,
            source = domain.source,
            telemetryRecordId = domain.telemetryRecordId,
            faultEventId = domain.faultEventId,
            maintenanceScheduleId = domain.maintenanceScheduleId,
            maintenanceRecordId = domain.maintenanceRecordId,
            title = domain.title,
            description = domain.description,
            correlationKey = domain.correlationKey,
            notificationId = domain.notificationId,
            occurredAt = domain.occurredAt,
            acknowledgedAt = domain.acknowledgedAt,
            acknowledgedBy = domain.acknowledgedBy,
            resolvedAt = domain.resolvedAt,
            resolvedBy = domain.resolvedBy,
            resolutionNotes = domain.resolutionNotes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            createdBy = domain.createdBy,
            updatedBy = domain.updatedBy
        )
    }
}

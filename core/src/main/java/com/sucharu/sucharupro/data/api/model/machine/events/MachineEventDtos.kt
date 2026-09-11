package com.sucharu.sucharupro.data.api.model.machine.events

import com.sucharu.sucharupro.domain.machine.events.DowntimeReasonCategory
import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.FaultSeverity
import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent

/**
 * REST API DTOs for Machine Fault Events and Downtime Events.
 */
data class CreateFaultEventRequestDto(
    val faultCode: String? = null,
    val faultType: String,
    val severity: FaultSeverity = FaultSeverity.FAULT,
    val description: String,
    val source: String = "MANUAL",
    val occurredAt: Long? = null,
    val maintenanceRecordId: String? = null,
    val metadataJson: String? = null
)

data class ResolveFaultEventRequestDto(
    val resolutionNotes: String? = null,
    val maintenanceRecordId: String? = null
)

data class StartDowntimeEventRequestDto(
    val faultEventId: String? = null,
    val executionJobId: String? = null,
    val workOrderId: String? = null,
    val reasonCategory: DowntimeReasonCategory,
    val reasonDetails: String? = null,
    val startedAt: Long? = null
)

data class MachineFaultEventResponseDto(
    val faultEventId: String,
    val tenantId: String,
    val machineId: String,
    val faultCode: String?,
    val faultType: String,
    val severity: FaultSeverity,
    val status: FaultStatus,
    val description: String,
    val source: String,
    val occurredAt: Long,
    val detectedAt: Long,
    val acknowledgedAt: Long?,
    val acknowledgedBy: String?,
    val resolvedAt: Long?,
    val resolvedBy: String?,
    val resolutionNotes: String?,
    val maintenanceRecordId: String?,
    val metadataJson: String?,
    val correlationId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String?,
    val updatedBy: String?
) {
    companion object {
        fun fromDomain(domain: MachineFaultEvent): MachineFaultEventResponseDto = MachineFaultEventResponseDto(
            faultEventId = domain.faultEventId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            faultCode = domain.faultCode,
            faultType = domain.faultType,
            severity = domain.severity,
            status = domain.status,
            description = domain.description,
            source = domain.source,
            occurredAt = domain.occurredAt,
            detectedAt = domain.detectedAt,
            acknowledgedAt = domain.acknowledgedAt,
            acknowledgedBy = domain.acknowledgedBy,
            resolvedAt = domain.resolvedAt,
            resolvedBy = domain.resolvedBy,
            resolutionNotes = domain.resolutionNotes,
            maintenanceRecordId = domain.maintenanceRecordId,
            metadataJson = domain.metadataJson,
            correlationId = domain.correlationId,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            createdBy = domain.createdBy,
            updatedBy = domain.updatedBy
        )
    }
}

data class MachineDowntimeEventResponseDto(
    val downtimeId: String,
    val tenantId: String,
    val machineId: String,
    val faultEventId: String?,
    val executionJobId: String?,
    val workOrderId: String?,
    val reasonCategory: DowntimeReasonCategory,
    val reasonDetails: String?,
    val status: DowntimeStatus,
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String?,
    val updatedBy: String?
) {
    companion object {
        fun fromDomain(domain: MachineDowntimeEvent): MachineDowntimeEventResponseDto = MachineDowntimeEventResponseDto(
            downtimeId = domain.downtimeId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            faultEventId = domain.faultEventId,
            executionJobId = domain.executionJobId,
            workOrderId = domain.workOrderId,
            reasonCategory = domain.reasonCategory,
            reasonDetails = domain.reasonDetails,
            status = domain.status,
            startedAt = domain.startedAt,
            endedAt = domain.endedAt,
            durationSeconds = domain.durationSeconds,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            createdBy = domain.createdBy,
            updatedBy = domain.updatedBy
        )
    }
}

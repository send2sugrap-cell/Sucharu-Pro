package com.sucharu.sucharupro.data.api.model.machine.maintenance

import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceType

/**
 * REST API DTOs for Machine Maintenance Management.
 */
data class CreateMaintenanceScheduleRequestDto(
    val title: String,
    val description: String? = null,
    val maintenanceType: MaintenanceType = MaintenanceType.PREVENTIVE,
    val plannedDate: Long,
    val recurrenceIntervalDays: Int? = null,
    val assignedTechnicianId: String? = null,
    val assignedTechnicianName: String? = null,
    val notes: String? = null
)

data class CreateMaintenanceRecordRequestDto(
    val scheduleId: String? = null,
    val title: String,
    val problemDescription: String? = null,
    val maintenanceType: MaintenanceType = MaintenanceType.CORRECTIVE,
    val performedById: String? = null,
    val performedByName: String? = null,
    val notes: String? = null
)

data class CompleteMaintenanceRecordRequestDto(
    val resolutionSummary: String? = null,
    val workPerformed: String? = null
)

data class CancelMaintenanceRecordRequestDto(
    val reason: String? = null
)

data class MaintenanceScheduleResponseDto(
    val scheduleId: String,
    val tenantId: String,
    val machineId: String,
    val title: String,
    val description: String?,
    val maintenanceType: MaintenanceType,
    val status: MaintenanceStatus,
    val plannedDate: Long,
    val recurrenceIntervalDays: Int?,
    val assignedTechnicianId: String?,
    val assignedTechnicianName: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String?,
    val updatedBy: String?
) {
    companion object {
        fun fromDomain(domain: MaintenanceSchedule): MaintenanceScheduleResponseDto = MaintenanceScheduleResponseDto(
            scheduleId = domain.scheduleId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            title = domain.title,
            description = domain.description,
            maintenanceType = domain.maintenanceType,
            status = domain.status,
            plannedDate = domain.plannedDate,
            recurrenceIntervalDays = domain.recurrenceIntervalDays,
            assignedTechnicianId = domain.assignedTechnicianId,
            assignedTechnicianName = domain.assignedTechnicianName,
            notes = domain.notes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            createdBy = domain.createdBy,
            updatedBy = domain.updatedBy
        )
    }
}

data class MaintenanceRecordResponseDto(
    val recordId: String,
    val tenantId: String,
    val machineId: String,
    val scheduleId: String?,
    val title: String,
    val problemDescription: String?,
    val workPerformed: String?,
    val maintenanceType: MaintenanceType,
    val status: MaintenanceStatus,
    val openedAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val performedById: String?,
    val performedByName: String?,
    val resolutionSummary: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String?,
    val updatedBy: String?
) {
    companion object {
        fun fromDomain(domain: MaintenanceRecord): MaintenanceRecordResponseDto = MaintenanceRecordResponseDto(
            recordId = domain.recordId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            scheduleId = domain.scheduleId,
            title = domain.title,
            problemDescription = domain.problemDescription,
            workPerformed = domain.workPerformed,
            maintenanceType = domain.maintenanceType,
            status = domain.status,
            openedAt = domain.openedAt,
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            performedById = domain.performedById,
            performedByName = domain.performedByName,
            resolutionSummary = domain.resolutionSummary,
            notes = domain.notes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            createdBy = domain.createdBy,
            updatedBy = domain.updatedBy
        )
    }
}

data class ServiceHistoryLogResponseDto(
    val historyId: String,
    val tenantId: String,
    val machineId: String,
    val recordId: String,
    val actionType: String,
    val performedById: String?,
    val performedByName: String?,
    val detailsJson: String?,
    val recordedAt: Long
) {
    companion object {
        fun fromDomain(domain: MachineServiceHistoryLog): ServiceHistoryLogResponseDto = ServiceHistoryLogResponseDto(
            historyId = domain.historyId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            recordId = domain.recordId,
            actionType = domain.actionType,
            performedById = domain.performedById,
            performedByName = domain.performedByName,
            detailsJson = domain.detailsJson,
            recordedAt = domain.recordedAt
        )
    }
}

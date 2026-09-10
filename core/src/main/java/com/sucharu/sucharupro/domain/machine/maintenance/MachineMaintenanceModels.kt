package com.sucharu.sucharupro.domain.machine.maintenance

/**
 * Maintenance types for registered machines.
 */
enum class MaintenanceType {
    PREVENTIVE,
    CORRECTIVE
}

/**
 * Lifecycle status of maintenance schedules and records.
 */
enum class MaintenanceStatus {
    PLANNED,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    OVERDUE;

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELLED
}

/**
 * Scheduled maintenance item.
 */
data class MaintenanceSchedule(
    val scheduleId: String,
    val tenantId: String,
    val machineId: String,
    val title: String,
    val description: String? = null,
    val maintenanceType: MaintenanceType,
    val status: MaintenanceStatus = MaintenanceStatus.PLANNED,
    val plannedDate: Long,
    val recurrenceIntervalDays: Int? = null,
    val assignedTechnicianId: String? = null,
    val assignedTechnicianName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null
)

/**
 * Executed/Active maintenance record.
 */
data class MaintenanceRecord(
    val recordId: String,
    val tenantId: String,
    val machineId: String,
    val scheduleId: String? = null,
    val title: String,
    val problemDescription: String? = null,
    val workPerformed: String? = null,
    val maintenanceType: MaintenanceType,
    val status: MaintenanceStatus = MaintenanceStatus.PLANNED,
    val openedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val performedById: String? = null,
    val performedByName: String? = null,
    val resolutionSummary: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null
)

/**
 * Immutable service history log for machine maintenance events.
 */
data class MachineServiceHistoryLog(
    val historyId: String,
    val tenantId: String,
    val machineId: String,
    val recordId: String,
    val actionType: String,
    val performedById: String? = null,
    val performedByName: String? = null,
    val detailsJson: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

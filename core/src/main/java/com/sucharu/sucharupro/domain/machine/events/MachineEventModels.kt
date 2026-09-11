package com.sucharu.sucharupro.domain.machine.events

/**
 * Severity level for machine fault events.
 */
enum class FaultSeverity {
    WARNING,
    FAULT,
    CRITICAL
}

/**
 * Lifecycle status for machine fault events.
 */
enum class FaultStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED,
    CANCELLED;

    val isTerminal: Boolean get() = this == RESOLVED || this == CANCELLED
}

/**
 * Classification of downtime root cause reasons.
 */
enum class DowntimeReasonCategory {
    MACHINE_FAULT,
    MAINTENANCE,
    POWER,
    MATERIAL_WAIT,
    OPERATOR,
    SETUP,
    OTHER
}

/**
 * Status lifecycle of downtime events.
 */
enum class DowntimeStatus {
    STARTED,
    ENDED,
    CANCELLED;

    val isTerminal: Boolean get() = this == ENDED || this == CANCELLED
}

/**
 * Machine Fault Event domain entity.
 */
data class MachineFaultEvent(
    val faultEventId: String,
    val tenantId: String,
    val machineId: String,
    val faultCode: String? = null,
    val faultType: String,
    val severity: FaultSeverity = FaultSeverity.FAULT,
    val status: FaultStatus = FaultStatus.OPEN,
    val description: String,
    val source: String = "MANUAL",
    val occurredAt: Long = System.currentTimeMillis(),
    val detectedAt: Long = System.currentTimeMillis(),
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val maintenanceRecordId: String? = null,
    val metadataJson: String? = null,
    val correlationId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null
)

/**
 * Machine Downtime Event domain entity.
 */
data class MachineDowntimeEvent(
    val downtimeId: String,
    val tenantId: String,
    val machineId: String,
    val faultEventId: String? = null,
    val executionJobId: String? = null,
    val workOrderId: String? = null,
    val reasonCategory: DowntimeReasonCategory,
    val reasonDetails: String? = null,
    val status: DowntimeStatus = DowntimeStatus.STARTED,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val durationSeconds: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null
)

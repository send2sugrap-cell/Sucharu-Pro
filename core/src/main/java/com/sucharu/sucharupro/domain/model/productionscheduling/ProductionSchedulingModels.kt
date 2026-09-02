package com.sucharu.sucharupro.domain.model.productionscheduling

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

// ============================================================
// ENUMS (Module 17 Step 06)
// ============================================================

enum class ScheduleStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PROPOSED("Proposed"),
    APPROVED("Approved"),
    DISPATCHED("Dispatched"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    SUPERSEDED("Superseded"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean get() = this == COMPLETED || this == SUPERSEDED || this == CANCELLED
    val isActionable: Boolean get() = this == DRAFT || this == PROPOSED || this == APPROVED || this == DISPATCHED || this == ACTIVE

    fun canTransitionTo(target: ScheduleStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            DRAFT -> target == PROPOSED || target == APPROVED || target == CANCELLED
            PROPOSED -> target == APPROVED || target == DRAFT || target == CANCELLED
            APPROVED -> target == DISPATCHED || target == ACTIVE || target == SUPERSEDED || target == CANCELLED
            DISPATCHED -> target == ACTIVE || target == SUPERSEDED || target == CANCELLED
            ACTIVE -> target == COMPLETED || target == SUPERSEDED || target == CANCELLED
            COMPLETED -> false
            SUPERSEDED -> false
            CANCELLED -> false
        }
    }
}

enum class DispatchStatus(val defaultLabel: String) {
    QUEUED("Queued"),
    READY("Ready"),
    DISPATCHED("Dispatched"),
    ACKNOWLEDGED("Acknowledged"),
    RUNNING("Running"),
    BLOCKED("Blocked"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELLED
}

enum class ConflictSeverity {
    CRITICAL_BLOCKING,
    WARNING,
    INFO
}

enum class ScheduleConflictType {
    MACHINE_DOUBLE_BOOKED,
    OPERATOR_DOUBLE_BOOKED,
    MACHINE_UNAVAILABLE,
    MAINTENANCE_OVERLAP,
    DEPENDENCY_VIOLATION,
    READINESS_BLOCKER,
    HOLD_CONFLICT,
    DUE_DATE_RISK,
    CAPACITY_EXCEEDED,
    OPERATOR_UNAVAILABLE,
    INVALID_STAGE_SEQUENCE
}

enum class ShiftType {
    MORNING_SHIFT,    // 08:00 - 16:00
    EVENING_SHIFT,    // 16:00 - 24:00
    NIGHT_SHIFT,      // 00:00 - 08:00
    FULL_DAY_24H      // 24 Hour continuous
}

enum class ProductionSchedulingEventType {
    SCHEDULE_CREATED,
    SCHEDULE_PROPOSED,
    SCHEDULE_APPROVED,
    SCHEDULE_RECALCULATED,
    SCHEDULE_SUPERSEDED,
    SCHEDULE_CANCELLED,
    WORK_ORDER_QUEUED,
    WORK_ORDER_DISPATCHED,
    WORK_ORDER_ACKNOWLEDGED,
    CONFLICT_DETECTED,
    RECONCILIATION_PERFORMED
}

// ============================================================
// DOMAIN ENTITIES
// ============================================================

/**
 * Single scheduled operational time slot for a Work Order.
 */
data class ProductionScheduleSlot(
    val slotId: String,
    val scheduleId: String,
    val workOrderId: String,
    val executionJobId: String,
    val sequenceNumber: Int,
    val stageType: ProductionStageType,
    val operationCode: String,
    val operationName: String,
    val machineId: String,
    val machineName: String,
    val operatorId: String?,
    val operatorName: String?,
    val scheduledStartTimestamp: Long,
    val scheduledEndTimestamp: Long,
    val setupMinutes: Int,
    val runMinutes: Int,
    val totalEstimatedMinutes: Int = setupMinutes + runMinutes,
    val priorityScore: BigDecimal,
    val status: DispatchStatus = DispatchStatus.QUEUED,
    val notes: String? = null
)

/**
 * Deterministic Machine Capacity Window.
 */
data class ProductionCapacityWindow(
    val windowId: String,
    val tenantId: String,
    val machineId: String,
    val machineName: String,
    val shiftDate: String,                 // YYYY-MM-DD
    val shiftType: ShiftType,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val totalCapacityMinutes: BigDecimal,  // e.g. 480.0000
    val allocatedMinutes: BigDecimal,      // e.g. 240.0000
    val availableMinutes: BigDecimal,      // e.g. 240.0000
    val utilizationRate: BigDecimal        // e.g. 0.5000 (50.0000%)
)

/**
 * Dispatch Queue Item bridging scheduled work orders to the shop floor.
 */
data class ProductionDispatchQueueItem(
    val queueItemId: String,
    val tenantId: String,
    val scheduleId: String,
    val scheduleVersion: Int,
    val workOrderId: String,
    val executionJobId: String,
    val orderId: String,
    val orderNumber: String,
    val sequenceNumber: Int,
    val stageType: ProductionStageType,
    val operationCode: String,
    val operationName: String,
    val targetWorkCenter: String,
    val machineId: String,
    val machineName: String,
    val operatorId: String?,
    val operatorName: String?,
    val dispatchStatus: DispatchStatus = DispatchStatus.QUEUED,
    val priorityScore: BigDecimal,
    val plannedQuantity: BigDecimal,
    val estimatedSetupMinutes: Int,
    val estimatedRunMinutes: Int,
    val scheduledStartTimestamp: Long,
    val scheduledEndTimestamp: Long,
    val queuedAt: Long,
    val readyAt: Long? = null,
    val dispatchedAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val completedAt: Long? = null,
    val notes: String? = null
)

/**
 * Machine availability and operational profile.
 */
data class ProductionMachineAvailability(
    val machineId: String,
    val machineName: String,
    val workCenter: String,
    val isOnline: Boolean = true,
    val shiftHoursPerDay: BigDecimal = BigDecimal("16.0000"), // 2 shifts standard
    val hourlyOutputRate: BigDecimal = BigDecimal("5000.0000"),
    val supportedStageTypes: List<ProductionStageType> = emptyList(),
    val maintenanceBlocks: List<ProductionMaintenanceBlock> = emptyList()
)

/**
 * Planned machine maintenance window.
 */
data class ProductionMaintenanceBlock(
    val blockId: String,
    val machineId: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val reason: String,
    val isBlocking: Boolean = true
)

/**
 * Operator availability profile.
 */
data class ProductionOperatorAvailability(
    val operatorId: String,
    val operatorName: String,
    val isActive: Boolean = true,
    val qualifiedStages: List<ProductionStageType> = emptyList(),
    val shiftType: ShiftType = ShiftType.MORNING_SHIFT,
    val assignedSlotCount: Int = 0
)

/**
 * Actionable Capacity & Scheduling Conflict.
 */
data class ProductionScheduleConflict(
    val conflictId: String,
    val scheduleId: String,
    val conflictType: ScheduleConflictType,
    val severity: ConflictSeverity,
    val workOrderId: String?,
    val machineId: String?,
    val operatorId: String?,
    val message: String,
    val isBlocking: Boolean,
    val recommendedAction: String
)

/**
 * Append-only lifecycle audit event.
 */
data class ProductionScheduleEvent(
    val eventId: String,
    val scheduleId: String,
    val tenantId: String,
    val eventType: ProductionSchedulingEventType,
    val fromStatus: ScheduleStatus? = null,
    val toStatus: ScheduleStatus? = null,
    val payload: String? = null,
    val performedBy: String,
    val performedAt: Long = System.currentTimeMillis()
)

/**
 * Authoritative Production Schedule aggregate root.
 */
data class ProductionSchedule(
    val scheduleId: String,
    val tenantId: String,
    val projectId: String,
    val executionJobId: String,
    val orderId: String,
    val orderNumber: String,
    val version: Int = 1,
    val isCurrent: Boolean = true,
    val status: ScheduleStatus = ScheduleStatus.PROPOSED,
    val plannedStartAt: Long,
    val plannedEndAt: Long,
    val totalSetupMinutes: Int,
    val totalRunMinutes: Int,
    val totalEstimatedMinutes: Int = totalSetupMinutes + totalRunMinutes,
    val slots: List<ProductionScheduleSlot> = emptyList(),
    val capacityWindows: List<ProductionCapacityWindow> = emptyList(),
    val conflicts: List<ProductionScheduleConflict> = emptyList(),
    val scheduleFingerprint: String,
    val integrityHash: String,
    val supersededByScheduleId: String? = null,
    val supersedingReason: String? = null,
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String? = null
) {
    val hasBlockingConflicts: Boolean get() = conflicts.any { it.isBlocking }
    val isApproved: Boolean get() = status == ScheduleStatus.APPROVED || status == ScheduleStatus.DISPATCHED || status == ScheduleStatus.ACTIVE
}

/**
 * 8-Way Multi-Tier Reconciliation Result:
 * Order ↔ Commitment ↔ Quotation ↔ Planning ↔ Job Execution ↔ Work Order ↔ Schedule ↔ Dispatch.
 */
data class ProductionScheduleReconciliationResult(
    val scheduleId: String,
    val executionJobId: String,
    val orderId: String,
    val isFullyReconciled: Boolean,
    val planningMatch: Boolean,
    val executionJobMatch: Boolean,
    val workOrdersMatched: Boolean,
    val slotsComplete: Boolean,
    val capacityFeasible: Boolean,
    val zeroBlockingConflicts: Boolean,
    val dispatchAligned: Boolean,
    val tenantIsolationVerified: Boolean,
    val discrepancies: List<String> = emptyList(),
    val reconciledAt: Long = System.currentTimeMillis()
)

/**
 * Read-only AI Agent Handoff Contract for Module 17 Step 06.
 */
data class Module17Step06ProductionSchedulingHandoffContract(
    val contractVersion: String = "1.0.0",
    val scheduleId: String,
    val scheduleVersion: Int,
    val tenantId: String,
    val projectId: String,
    val executionJobId: String,
    val orderId: String,
    val orderNumber: String,
    val status: String,
    val plannedStartAt: Long,
    val plannedEndAt: Long,
    val totalEstimatedDurationMinutes: Int,
    val slotsCount: Int,
    val machineAssignmentsSummary: List<String>,
    val operatorAssignmentsSummary: List<String>,
    val capacityUtilizationAvg: BigDecimal,
    val activeConflictsCount: Int,
    val blockingIssues: List<String>,
    val warnings: List<String>,
    val dispatchStatusSummary: Map<String, Int>,
    val isFullyReconciled: Boolean,
    val integrityHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)

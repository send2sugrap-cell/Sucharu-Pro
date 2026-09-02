package com.sucharu.sucharupro.data.api.model.productionscheduling

import com.sucharu.sucharupro.domain.model.productionscheduling.*
import java.math.BigDecimal

data class CreateProductionScheduleRequestDto(
    val baseStartTime: Long? = null,
    val requestedDueDate: Long? = null
)

data class SupersedeProductionScheduleRequestDto(
    val reason: String,
    val newStartTime: Long? = null,
    val requestedDueDate: Long? = null
)

data class ProductionScheduleSlotDto(
    val slotId: String,
    val scheduleId: String,
    val workOrderId: String,
    val executionJobId: String,
    val sequenceNumber: Int,
    val stageType: String,
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
    val totalEstimatedMinutes: Int,
    val priorityScore: BigDecimal,
    val status: String,
    val notes: String?
)

data class ProductionCapacityWindowDto(
    val windowId: String,
    val tenantId: String,
    val machineId: String,
    val machineName: String,
    val shiftDate: String,
    val shiftType: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val totalCapacityMinutes: BigDecimal,
    val allocatedMinutes: BigDecimal,
    val availableMinutes: BigDecimal,
    val utilizationRate: BigDecimal
)

data class ProductionDispatchQueueItemDto(
    val queueItemId: String,
    val tenantId: String,
    val scheduleId: String,
    val scheduleVersion: Int,
    val workOrderId: String,
    val executionJobId: String,
    val orderId: String,
    val orderNumber: String,
    val sequenceNumber: Int,
    val stageType: String,
    val operationCode: String,
    val operationName: String,
    val targetWorkCenter: String,
    val machineId: String,
    val machineName: String,
    val operatorId: String?,
    val operatorName: String?,
    val dispatchStatus: String,
    val priorityScore: BigDecimal,
    val plannedQuantity: BigDecimal,
    val estimatedSetupMinutes: Int,
    val estimatedRunMinutes: Int,
    val scheduledStartTimestamp: Long,
    val scheduledEndTimestamp: Long,
    val queuedAt: Long,
    val readyAt: Long?,
    val dispatchedAt: Long?,
    val acknowledgedAt: Long?,
    val completedAt: Long?,
    val notes: String?
)

data class ProductionScheduleConflictDto(
    val conflictId: String,
    val scheduleId: String,
    val conflictType: String,
    val severity: String,
    val workOrderId: String?,
    val machineId: String?,
    val operatorId: String?,
    val message: String,
    val isBlocking: Boolean,
    val recommendedAction: String
)

data class ProductionScheduleResponseDto(
    val scheduleId: String,
    val tenantId: String,
    val projectId: String,
    val executionJobId: String,
    val orderId: String,
    val orderNumber: String,
    val version: Int,
    val isCurrent: Boolean,
    val status: String,
    val plannedStartAt: Long,
    val plannedEndAt: Long,
    val totalSetupMinutes: Int,
    val totalRunMinutes: Int,
    val totalEstimatedMinutes: Int,
    val slots: List<ProductionScheduleSlotDto>,
    val capacityWindows: List<ProductionCapacityWindowDto>,
    val conflicts: List<ProductionScheduleConflictDto>,
    val scheduleFingerprint: String,
    val integrityHash: String,
    val supersededByScheduleId: String?,
    val supersedingReason: String?,
    val approvedAt: Long?,
    val approvedBy: String?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?
)

data class ProductionScheduleReconciliationResponseDto(
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
    val discrepancies: List<String>,
    val reconciledAt: Long
)

data class Module17Step06ProductionSchedulingHandoffContractDto(
    val contractVersion: String,
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
    val generatedAt: Long
)

// Mapping Extension Functions
fun ProductionSchedule.toDto(): ProductionScheduleResponseDto = ProductionScheduleResponseDto(
    scheduleId = scheduleId,
    tenantId = tenantId,
    projectId = projectId,
    executionJobId = executionJobId,
    orderId = orderId,
    orderNumber = orderNumber,
    version = version,
    isCurrent = isCurrent,
    status = status.name,
    plannedStartAt = plannedStartAt,
    plannedEndAt = plannedEndAt,
    totalSetupMinutes = totalSetupMinutes,
    totalRunMinutes = totalRunMinutes,
    totalEstimatedMinutes = totalEstimatedMinutes,
    slots = slots.map { it.toDto() },
    capacityWindows = capacityWindows.map { it.toDto() },
    conflicts = conflicts.map { it.toDto() },
    scheduleFingerprint = scheduleFingerprint,
    integrityHash = integrityHash,
    supersededByScheduleId = supersededByScheduleId,
    supersedingReason = supersedingReason,
    approvedAt = approvedAt,
    approvedBy = approvedBy,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy
)

fun ProductionScheduleSlot.toDto(): ProductionScheduleSlotDto = ProductionScheduleSlotDto(
    slotId = slotId,
    scheduleId = scheduleId,
    workOrderId = workOrderId,
    executionJobId = executionJobId,
    sequenceNumber = sequenceNumber,
    stageType = stageType.name,
    operationCode = operationCode,
    operationName = operationName,
    machineId = machineId,
    machineName = machineName,
    operatorId = operatorId,
    operatorName = operatorName,
    scheduledStartTimestamp = scheduledStartTimestamp,
    scheduledEndTimestamp = scheduledEndTimestamp,
    setupMinutes = setupMinutes,
    runMinutes = runMinutes,
    totalEstimatedMinutes = totalEstimatedMinutes,
    priorityScore = priorityScore,
    status = status.name,
    notes = notes
)

fun ProductionCapacityWindow.toDto(): ProductionCapacityWindowDto = ProductionCapacityWindowDto(
    windowId = windowId,
    tenantId = tenantId,
    machineId = machineId,
    machineName = machineName,
    shiftDate = shiftDate,
    shiftType = shiftType.name,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    totalCapacityMinutes = totalCapacityMinutes,
    allocatedMinutes = allocatedMinutes,
    availableMinutes = availableMinutes,
    utilizationRate = utilizationRate
)

fun ProductionDispatchQueueItem.toDto(): ProductionDispatchQueueItemDto = ProductionDispatchQueueItemDto(
    queueItemId = queueItemId,
    tenantId = tenantId,
    scheduleId = scheduleId,
    scheduleVersion = scheduleVersion,
    workOrderId = workOrderId,
    executionJobId = executionJobId,
    orderId = orderId,
    orderNumber = orderNumber,
    sequenceNumber = sequenceNumber,
    stageType = stageType.name,
    operationCode = operationCode,
    operationName = operationName,
    targetWorkCenter = targetWorkCenter,
    machineId = machineId,
    machineName = machineName,
    operatorId = operatorId,
    operatorName = operatorName,
    dispatchStatus = dispatchStatus.name,
    priorityScore = priorityScore,
    plannedQuantity = plannedQuantity,
    estimatedSetupMinutes = estimatedSetupMinutes,
    estimatedRunMinutes = estimatedRunMinutes,
    scheduledStartTimestamp = scheduledStartTimestamp,
    scheduledEndTimestamp = scheduledEndTimestamp,
    queuedAt = queuedAt,
    readyAt = readyAt,
    dispatchedAt = dispatchedAt,
    acknowledgedAt = acknowledgedAt,
    completedAt = completedAt,
    notes = notes
)

fun ProductionScheduleConflict.toDto(): ProductionScheduleConflictDto = ProductionScheduleConflictDto(
    conflictId = conflictId,
    scheduleId = scheduleId,
    conflictType = conflictType.name,
    severity = severity.name,
    workOrderId = workOrderId,
    machineId = machineId,
    operatorId = operatorId,
    message = message,
    isBlocking = isBlocking,
    recommendedAction = recommendedAction
)

fun ProductionScheduleReconciliationResult.toDto(): ProductionScheduleReconciliationResponseDto = ProductionScheduleReconciliationResponseDto(
    scheduleId = scheduleId,
    executionJobId = executionJobId,
    orderId = orderId,
    isFullyReconciled = isFullyReconciled,
    planningMatch = planningMatch,
    executionJobMatch = executionJobMatch,
    workOrdersMatched = workOrdersMatched,
    slotsComplete = slotsComplete,
    capacityFeasible = capacityFeasible,
    zeroBlockingConflicts = zeroBlockingConflicts,
    dispatchAligned = dispatchAligned,
    tenantIsolationVerified = tenantIsolationVerified,
    discrepancies = discrepancies,
    reconciledAt = reconciledAt
)

fun Module17Step06ProductionSchedulingHandoffContract.toDto(): Module17Step06ProductionSchedulingHandoffContractDto = Module17Step06ProductionSchedulingHandoffContractDto(
    contractVersion = contractVersion,
    scheduleId = scheduleId,
    scheduleVersion = scheduleVersion,
    tenantId = tenantId,
    projectId = projectId,
    executionJobId = executionJobId,
    orderId = orderId,
    orderNumber = orderNumber,
    status = status,
    plannedStartAt = plannedStartAt,
    plannedEndAt = plannedEndAt,
    totalEstimatedDurationMinutes = totalEstimatedDurationMinutes,
    slotsCount = slotsCount,
    machineAssignmentsSummary = machineAssignmentsSummary,
    operatorAssignmentsSummary = operatorAssignmentsSummary,
    capacityUtilizationAvg = capacityUtilizationAvg,
    activeConflictsCount = activeConflictsCount,
    blockingIssues = blockingIssues,
    warnings = warnings,
    dispatchStatusSummary = dispatchStatusSummary,
    isFullyReconciled = isFullyReconciled,
    integrityHash = integrityHash,
    generatedAt = generatedAt
)

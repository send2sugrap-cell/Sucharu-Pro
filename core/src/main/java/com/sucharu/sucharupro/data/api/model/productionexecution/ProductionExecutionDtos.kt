package com.sucharu.sucharupro.data.api.model.productionexecution

import com.sucharu.sucharupro.data.api.model.productionplanning.ProductionJobSpecificationDto
import com.sucharu.sucharupro.data.api.model.productionplanning.toDto
import com.sucharu.sucharupro.domain.model.productionexecution.*
import java.math.BigDecimal

data class CreateProductionJobRequest(
    val orderId: String,
    val idempotencyKey: String? = null
)

data class StartStageRequest(
    val operatorId: String? = null,
    val machineId: String? = null
)

data class PauseStageRequest(
    val reason: String? = null
)

data class CompleteStageRequest(
    val goodQuantity: String,
    val scrapQuantity: String = "0.0000",
    val notes: String? = null
)

data class AssignMachineRequest(
    val machineId: String,
    val machineName: String
)

data class AssignOperatorRequest(
    val operatorId: String,
    val operatorName: String
)

data class HoldJobRequest(
    val workOrderId: String? = null,
    val category: String, // HoldCategory
    val reason: String
)

data class ReleaseHoldRequest(
    val resolutionNotes: String? = null
)

data class RecordWastageRequest(
    val workOrderId: String,
    val materialCode: String,
    val quantity: String,
    val unitOfMeasure: String = "SHEETS",
    val reason: String
)

data class CreateReworkRequest(
    val sourceWorkOrderId: String,
    val targetWorkOrderId: String,
    val quantity: String,
    val defectCode: String? = null,
    val reason: String
)

data class CompleteJobRequest(
    val summary: String? = null
)

data class CancelJobRequest(
    val reason: String
)

// Response DTOs
data class ProductionJobExecutionDto(
    val executionJobId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val orderItemId: String,
    val customerId: String,
    val quotationId: String?,
    val quotationVersionNumber: Int?,
    val commercialCommitmentId: String?,
    val planningId: String,
    val planningVersion: Int,
    val title: String,
    val priority: String,
    val status: String,
    val specification: ProductionJobSpecificationDto,
    val plannedQuantity: String,
    val startedQuantity: String,
    val completedQuantity: String,
    val rejectedQuantity: String,
    val wastageQuantity: String,
    val reworkQuantity: String,
    val remainingQuantity: String,
    val workOrders: List<ProductionWorkOrderDto>,
    val currentHold: ProductionHoldDto?,
    val currentStageType: String?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val completionSummary: String?,
    val progressFraction: Float,
    val jobFingerprint: String,
    val integrityHash: String,
    val version: Int,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?
)

data class ProductionWorkOrderDto(
    val workOrderId: String,
    val executionJobId: String,
    val sequenceNumber: Int,
    val stageType: String,
    val operationCode: String,
    val operationName: String,
    val targetWorkCenter: String,
    val status: String,
    val assignedMachineId: String?,
    val assignedMachineName: String?,
    val assignedOperatorId: String?,
    val assignedOperatorName: String?,
    val estimatedSetupMinutes: Int,
    val estimatedRunMinutes: Int,
    val actualSetupMinutes: Int,
    val actualRunMinutes: Int,
    val plannedQuantity: String,
    val completedQuantity: String,
    val rejectedQuantity: String,
    val wastageQuantity: String,
    val isMandatory: Boolean,
    val isQcCheckpoint: Boolean,
    val predecessorWorkOrderIds: List<String>,
    val startedAt: Long?,
    val pausedAt: Long?,
    val completedAt: Long?,
    val notes: String?
)

data class ProductionExecutionActualDto(
    val actualId: String,
    val executionJobId: String,
    val workOrderId: String,
    val stageType: String,
    val machineId: String?,
    val operatorId: String?,
    val startedAt: Long,
    val completedAt: Long?,
    val durationSeconds: Long?,
    val goodQuantity: String,
    val scrapQuantity: String,
    val reworkQuantity: String,
    val remarks: String?
)

data class ProductionHoldDto(
    val holdId: String,
    val executionJobId: String,
    val workOrderId: String?,
    val category: String,
    val reason: String,
    val heldAt: Long,
    val heldBy: String,
    val isResolved: Boolean,
    val resolvedAt: Long?,
    val resolvedBy: String?,
    val resolutionNotes: String?
)

data class ProductionWastageRecordDto(
    val wastageId: String,
    val executionJobId: String,
    val workOrderId: String,
    val materialCode: String,
    val quantity: String,
    val unitOfMeasure: String,
    val reason: String,
    val stageType: String,
    val recordedBy: String,
    val recordedAt: Long
)

data class ProductionReworkRecordDto(
    val reworkId: String,
    val executionJobId: String,
    val sourceWorkOrderId: String,
    val targetWorkOrderId: String,
    val quantity: String,
    val defectCode: String?,
    val reason: String,
    val status: String,
    val requestedBy: String,
    val requestedAt: Long,
    val resolvedAt: Long?
)

data class ProductionExecutionDiagnosticDto(
    val code: String,
    val message: String,
    val isBlocking: Boolean,
    val stageType: String?,
    val recommendedAction: String?
)

data class ProductionExecutionReconciliationDto(
    val executionJobId: String,
    val orderId: String,
    val isFullyReconciled: Boolean,
    val quotationMatch: Boolean,
    val commitmentMatch: Boolean,
    val orderMatch: Boolean,
    val planningMatch: Boolean,
    val workOrdersComplete: Boolean,
    val quantityBalanced: Boolean,
    val qcCheckpointsPassed: Boolean,
    val discrepancies: List<String>,
    val reconciledAt: Long
)

data class Module17Step05ProductionExecutionHandoffDto(
    val contractVersion: String,
    val executionJobId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val status: String,
    val currentStage: String?,
    val progressFraction: Float,
    val plannedQuantity: String,
    val completedQuantity: String,
    val wastageQuantity: String,
    val reworkQuantity: String,
    val completedWorkOrdersCount: Int,
    val totalWorkOrdersCount: Int,
    val hasActiveHold: Boolean,
    val holdReason: String?,
    val isFullyReconciled: Boolean,
    val isCompleted: Boolean,
    val integrityHash: String,
    val generatedAt: Long
)

data class ProductionExecutionEventDto(
    val eventId: String,
    val executionJobId: String,
    val workOrderId: String?,
    val eventType: String,
    val fromStatus: String?,
    val toStatus: String?,
    val payload: String?,
    val performedBy: String,
    val performedAt: Long
)

// Extension mappers
fun ProductionJobExecution.toDto(): ProductionJobExecutionDto = ProductionJobExecutionDto(
    executionJobId = executionJobId,
    tenantId = tenantId,
    projectId = projectId,
    orderId = orderId,
    orderNumber = orderNumber,
    orderItemId = orderItemId,
    customerId = customerId,
    quotationId = quotationId,
    quotationVersionNumber = quotationVersionNumber,
    commercialCommitmentId = commercialCommitmentId,
    planningId = planningId,
    planningVersion = planningVersion,
    title = title,
    priority = priority.name,
    status = status.name,
    specification = specification.toDto(),
    plannedQuantity = plannedQuantity.toPlainString(),
    startedQuantity = startedQuantity.toPlainString(),
    completedQuantity = completedQuantity.toPlainString(),
    rejectedQuantity = rejectedQuantity.toPlainString(),
    wastageQuantity = wastageQuantity.toPlainString(),
    reworkQuantity = reworkQuantity.toPlainString(),
    remainingQuantity = remainingQuantity.toPlainString(),
    workOrders = workOrders.map { it.toDto() },
    currentHold = currentHold?.toDto(),
    currentStageType = currentStageType?.name,
    isCompleted = isCompleted,
    completedAt = completedAt,
    completionSummary = completionSummary,
    progressFraction = progressFraction,
    jobFingerprint = jobFingerprint,
    integrityHash = integrityHash,
    version = version,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy
)

fun ProductionWorkOrder.toDto(): ProductionWorkOrderDto = ProductionWorkOrderDto(
    workOrderId = workOrderId,
    executionJobId = executionJobId,
    sequenceNumber = sequenceNumber,
    stageType = stageType.name,
    operationCode = operationCode,
    operationName = operationName,
    targetWorkCenter = targetWorkCenter,
    status = status.name,
    assignedMachineId = assignedMachineId,
    assignedMachineName = assignedMachineName,
    assignedOperatorId = assignedOperatorId,
    assignedOperatorName = assignedOperatorName,
    estimatedSetupMinutes = estimatedSetupMinutes,
    estimatedRunMinutes = estimatedRunMinutes,
    actualSetupMinutes = actualSetupMinutes,
    actualRunMinutes = actualRunMinutes,
    plannedQuantity = plannedQuantity.toPlainString(),
    completedQuantity = completedQuantity.toPlainString(),
    rejectedQuantity = rejectedQuantity.toPlainString(),
    wastageQuantity = wastageQuantity.toPlainString(),
    isMandatory = isMandatory,
    isQcCheckpoint = isQcCheckpoint,
    predecessorWorkOrderIds = predecessorWorkOrderIds,
    startedAt = startedAt,
    pausedAt = pausedAt,
    completedAt = completedAt,
    notes = notes
)

fun ProductionExecutionActual.toDto(): ProductionExecutionActualDto = ProductionExecutionActualDto(
    actualId = actualId,
    executionJobId = executionJobId,
    workOrderId = workOrderId,
    stageType = stageType.name,
    machineId = machineId,
    operatorId = operatorId,
    startedAt = startedAt,
    completedAt = completedAt,
    durationSeconds = durationSeconds,
    goodQuantity = goodQuantity.toPlainString(),
    scrapQuantity = scrapQuantity.toPlainString(),
    reworkQuantity = reworkQuantity.toPlainString(),
    remarks = remarks
)

fun ProductionHold.toDto(): ProductionHoldDto = ProductionHoldDto(
    holdId = holdId,
    executionJobId = executionJobId,
    workOrderId = workOrderId,
    category = category.name,
    reason = reason,
    heldAt = heldAt,
    heldBy = heldBy,
    isResolved = isResolved,
    resolvedAt = resolvedAt,
    resolvedBy = resolvedBy,
    resolutionNotes = resolutionNotes
)

fun ProductionWastageRecord.toDto(): ProductionWastageRecordDto = ProductionWastageRecordDto(
    wastageId = wastageId,
    executionJobId = executionJobId,
    workOrderId = workOrderId,
    materialCode = materialCode,
    quantity = quantity.toPlainString(),
    unitOfMeasure = unitOfMeasure,
    reason = reason,
    stageType = stageType.name,
    recordedBy = recordedBy,
    recordedAt = recordedAt
)

fun ProductionReworkRecord.toDto(): ProductionReworkRecordDto = ProductionReworkRecordDto(
    reworkId = reworkId,
    executionJobId = executionJobId,
    sourceWorkOrderId = sourceWorkOrderId,
    targetWorkOrderId = targetWorkOrderId,
    quantity = quantity.toPlainString(),
    defectCode = defectCode,
    reason = reason,
    status = status,
    requestedBy = requestedBy,
    requestedAt = requestedAt,
    resolvedAt = resolvedAt
)

fun ProductionExecutionDiagnostic.toDto(): ProductionExecutionDiagnosticDto = ProductionExecutionDiagnosticDto(
    code = code,
    message = message,
    isBlocking = isBlocking,
    stageType = stageType?.name,
    recommendedAction = recommendedAction
)

fun ProductionExecutionReconciliationResult.toDto(): ProductionExecutionReconciliationDto = ProductionExecutionReconciliationDto(
    executionJobId = executionJobId,
    orderId = orderId,
    isFullyReconciled = isFullyReconciled,
    quotationMatch = quotationMatch,
    commitmentMatch = commitmentMatch,
    orderMatch = orderMatch,
    planningMatch = planningMatch,
    workOrdersComplete = workOrdersComplete,
    quantityBalanced = quantityBalanced,
    qcCheckpointsPassed = qcCheckpointsPassed,
    discrepancies = discrepancies,
    reconciledAt = reconciledAt
)

fun Module17Step05ProductionExecutionHandoffContract.toDto(): Module17Step05ProductionExecutionHandoffDto = Module17Step05ProductionExecutionHandoffDto(
    contractVersion = contractVersion,
    executionJobId = executionJobId,
    tenantId = tenantId,
    projectId = projectId,
    orderId = orderId,
    orderNumber = orderNumber,
    customerId = customerId,
    status = status,
    currentStage = currentStage,
    progressFraction = progressFraction,
    plannedQuantity = plannedQuantity.toPlainString(),
    completedQuantity = completedQuantity.toPlainString(),
    wastageQuantity = wastageQuantity.toPlainString(),
    reworkQuantity = reworkQuantity.toPlainString(),
    completedWorkOrdersCount = completedWorkOrdersCount,
    totalWorkOrdersCount = totalWorkOrdersCount,
    hasActiveHold = hasActiveHold,
    holdReason = holdReason,
    isFullyReconciled = isFullyReconciled,
    isCompleted = isCompleted,
    integrityHash = integrityHash,
    generatedAt = generatedAt
)

fun ProductionExecutionEvent.toDto(): ProductionExecutionEventDto = ProductionExecutionEventDto(
    eventId = eventId,
    executionJobId = executionJobId,
    workOrderId = workOrderId,
    eventType = eventType.name,
    fromStatus = fromStatus,
    toStatus = toStatus,
    payload = payload,
    performedBy = performedBy,
    performedAt = performedAt
)

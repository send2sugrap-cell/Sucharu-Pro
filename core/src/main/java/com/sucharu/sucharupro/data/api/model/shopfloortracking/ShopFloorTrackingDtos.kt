package com.sucharu.sucharupro.data.api.model.shopfloortracking

import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import java.math.BigDecimal

data class StartWorkOrderExecutionRequestDto(
    val executionJobId: String,
    val orderId: String,
    val sequenceNumber: Int,
    val stageType: String,
    val machineId: String,
    val machineName: String,
    val operatorId: String,
    val operatorName: String,
    val isSetup: Boolean = true
)

data class PauseWorkOrderExecutionRequestDto(
    val pauseReason: String,
    val downtimeCategory: String? = null
)

data class RecordWorkOrderOutputRequestDto(
    val additionalGoodQuantity: BigDecimal = BigDecimal.ZERO,
    val additionalScrapQuantity: BigDecimal = BigDecimal.ZERO,
    val additionalSetupMinutes: Int = 0,
    val additionalRunMinutes: Int = 0,
    val additionalDowntimeMinutes: Int = 0,
    val isCompleted: Boolean = false
)

data class RecordMaterialConsumptionRequestDto(
    val executionJobId: String,
    val stageType: String,
    val materialCode: String,
    val materialName: String,
    val unitOfMeasure: String,
    val plannedQuantity: BigDecimal,
    val actualQuantity: BigDecimal,
    val scrapQuantity: BigDecimal = BigDecimal.ZERO,
    val batchLotNumber: String? = null,
    val notes: String? = null
)

data class LogMachineTelemetryRequestDto(
    val machineName: String,
    val workOrderId: String? = null,
    val executionJobId: String? = null,
    val recordedSpeedUnitsPerHour: BigDecimal,
    val ratedSpeedUnitsPerHour: BigDecimal,
    val totalImpressions: Long,
    val downtimeCategory: String? = null,
    val downtimeMinutes: Int = 0,
    val temperatureCelsius: BigDecimal? = null,
    val isRunning: Boolean = true
)

data class CreateStageHandoverRequestDto(
    val fromStage: String,
    val toWorkOrderId: String? = null,
    val toStage: String? = null,
    val plannedOutputQuantity: BigDecimal,
    val actualGoodQuantity: BigDecimal,
    val scrapQuantity: BigDecimal = BigDecimal.ZERO,
    val discrepancyNotes: String? = null
)

// Response DTOs
data class OperatorTimeTrackingResponseDto(
    val recordId: String,
    val tenantId: String,
    val workOrderId: String,
    val executionJobId: String,
    val orderId: String,
    val sequenceNumber: Int,
    val stageType: String,
    val machineId: String,
    val machineName: String,
    val operatorId: String,
    val operatorName: String,
    val currentState: String,
    val startedAt: Long?,
    val setupMinutes: Int,
    val runMinutes: Int,
    val downtimeMinutes: Int,
    val totalActiveMinutes: Int,
    val goodQuantityProduced: BigDecimal,
    val scrapQuantityProduced: BigDecimal,
    val pausedAt: Long?,
    val pauseReason: String?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

data class ProductionMaterialConsumptionResponseDto(
    val consumptionId: String,
    val tenantId: String,
    val workOrderId: String,
    val executionJobId: String,
    val stageType: String,
    val materialCode: String,
    val materialName: String,
    val unitOfMeasure: String,
    val plannedQuantity: BigDecimal,
    val actualQuantityConsumed: BigDecimal,
    val scrapQuantity: BigDecimal,
    val varianceQuantity: BigDecimal,
    val variancePercentage: BigDecimal,
    val batchLotNumber: String?,
    val status: String,
    val recordedBy: String,
    val recordedAt: Long,
    val notes: String?
)

data class MachineTelemetryResponseDto(
    val logId: String,
    val tenantId: String,
    val machineId: String,
    val machineName: String,
    val workOrderId: String?,
    val executionJobId: String?,
    val recordedSpeedUnitsPerHour: BigDecimal,
    val ratedSpeedUnitsPerHour: BigDecimal,
    val speedEfficiencyPercentage: BigDecimal,
    val totalImpressions: Long,
    val currentDowntimeCategory: String?,
    val downtimeMinutes: Int,
    val temperatureCelsius: BigDecimal?,
    val isRunning: Boolean,
    val loggedAt: Long,
    val loggedBy: String
)

data class StageOutputHandoverResponseDto(
    val handoverId: String,
    val tenantId: String,
    val executionJobId: String,
    val fromWorkOrderId: String,
    val fromStage: String,
    val toWorkOrderId: String?,
    val toStage: String?,
    val plannedOutputQuantity: BigDecimal,
    val actualGoodQuantity: BigDecimal,
    val scrapQuantity: BigDecimal,
    val yieldPercentage: BigDecimal,
    val handedOverBy: String,
    val handedOverAt: Long,
    val acceptedBy: String?,
    val acceptedAt: Long?,
    val status: String,
    val discrepancyNotes: String?,
    val integrityHash: String
)

data class ProductionExecutionVarianceResponseDto(
    val executionJobId: String,
    val tenantId: String,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val durationVarianceMinutes: Int,
    val durationEfficiencyRatio: BigDecimal,
    val plannedOutputQuantity: BigDecimal,
    val actualGoodOutputQuantity: BigDecimal,
    val totalScrapQuantity: BigDecimal,
    val overallYieldPercentage: BigDecimal,
    val totalPlannedMaterialCost: BigDecimal,
    val totalActualMaterialCost: BigDecimal,
    val materialCostVariance: BigDecimal,
    val averageMachineSpeedEfficiency: BigDecimal,
    val totalDowntimeMinutes: Int,
    val isWithinTolerance: Boolean,
    val generatedAt: Long
)

data class ShopFloorTrackingReconciliationResponseDto(
    val executionJobId: String,
    val tenantId: String,
    val isFullyReconciled: Boolean,
    val workOrdersMatched: Boolean,
    val timersConsistent: Boolean,
    val materialDepletionReconciled: Boolean,
    val telemetryLogged: Boolean,
    val handoversContinuous: Boolean,
    val zeroUnresolvedScrapDiscrepancies: Boolean,
    val cryptographicIntegrityPassed: Boolean,
    val discrepancies: List<String>,
    val reconciledAt: Long
)

data class Module17Step07ShopFloorTrackingHandoffContractDto(
    val contractVersion: String,
    val executionJobId: String,
    val orderId: String,
    val orderNumber: String,
    val tenantId: String,
    val totalStagesCount: Int,
    val completedStagesCount: Int,
    val overallYieldPercentage: BigDecimal,
    val speedEfficiencyPercentage: BigDecimal,
    val totalDowntimeMinutes: Int,
    val materialConsumptionsSummary: List<String>,
    val stageHandoversSummary: List<String>,
    val isFullyReconciled: Boolean,
    val integrityHash: String,
    val generatedAt: Long
)

// Mapping Extensions
fun OperatorTimeTrackingRecord.toDto(): OperatorTimeTrackingResponseDto = OperatorTimeTrackingResponseDto(
    recordId = recordId,
    tenantId = tenantId,
    workOrderId = workOrderId,
    executionJobId = executionJobId,
    orderId = orderId,
    sequenceNumber = sequenceNumber,
    stageType = stageType.name,
    machineId = machineId,
    machineName = machineName,
    operatorId = operatorId,
    operatorName = operatorName,
    currentState = currentState.name,
    startedAt = startedAt,
    setupMinutes = setupMinutes,
    runMinutes = runMinutes,
    downtimeMinutes = downtimeMinutes,
    totalActiveMinutes = totalActiveMinutes,
    goodQuantityProduced = goodQuantityProduced,
    scrapQuantityProduced = scrapQuantityProduced,
    pausedAt = pausedAt,
    pauseReason = pauseReason,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProductionMaterialConsumptionRecord.toDto(): ProductionMaterialConsumptionResponseDto = ProductionMaterialConsumptionResponseDto(
    consumptionId = consumptionId,
    tenantId = tenantId,
    workOrderId = workOrderId,
    executionJobId = executionJobId,
    stageType = stageType.name,
    materialCode = materialCode,
    materialName = materialName,
    unitOfMeasure = unitOfMeasure,
    plannedQuantity = plannedQuantity,
    actualQuantityConsumed = actualQuantityConsumed,
    scrapQuantity = scrapQuantity,
    varianceQuantity = varianceQuantity,
    variancePercentage = variancePercentage,
    batchLotNumber = batchLotNumber,
    status = status.name,
    recordedBy = recordedBy,
    recordedAt = recordedAt,
    notes = notes
)

fun MachineTelemetryLog.toDto(): MachineTelemetryResponseDto = MachineTelemetryResponseDto(
    logId = logId,
    tenantId = tenantId,
    machineId = machineId,
    machineName = machineName,
    workOrderId = workOrderId,
    executionJobId = executionJobId,
    recordedSpeedUnitsPerHour = recordedSpeedUnitsPerHour,
    ratedSpeedUnitsPerHour = ratedSpeedUnitsPerHour,
    speedEfficiencyPercentage = speedEfficiencyPercentage,
    totalImpressions = totalImpressions,
    currentDowntimeCategory = currentDowntimeCategory?.name,
    downtimeMinutes = downtimeMinutes,
    temperatureCelsius = temperatureCelsius,
    isRunning = isRunning,
    loggedAt = loggedAt,
    loggedBy = loggedBy
)

fun StageOutputHandoverRecord.toDto(): StageOutputHandoverResponseDto = StageOutputHandoverResponseDto(
    handoverId = handoverId,
    tenantId = tenantId,
    executionJobId = executionJobId,
    fromWorkOrderId = fromWorkOrderId,
    fromStage = fromStage.name,
    toWorkOrderId = toWorkOrderId,
    toStage = toStage?.name,
    plannedOutputQuantity = plannedOutputQuantity,
    actualGoodQuantity = actualGoodQuantity,
    scrapQuantity = scrapQuantity,
    yieldPercentage = yieldPercentage,
    handedOverBy = handedOverBy,
    handedOverAt = handedOverAt,
    acceptedBy = acceptedBy,
    acceptedAt = acceptedAt,
    status = status.name,
    discrepancyNotes = discrepancyNotes,
    integrityHash = integrityHash
)

fun ProductionExecutionVarianceSummary.toDto(): ProductionExecutionVarianceResponseDto = ProductionExecutionVarianceResponseDto(
    executionJobId = executionJobId,
    tenantId = tenantId,
    plannedDurationMinutes = plannedDurationMinutes,
    actualDurationMinutes = actualDurationMinutes,
    durationVarianceMinutes = durationVarianceMinutes,
    durationEfficiencyRatio = durationEfficiencyRatio,
    plannedOutputQuantity = plannedOutputQuantity,
    actualGoodOutputQuantity = actualGoodOutputQuantity,
    totalScrapQuantity = totalScrapQuantity,
    overallYieldPercentage = overallYieldPercentage,
    totalPlannedMaterialCost = totalPlannedMaterialCost,
    totalActualMaterialCost = totalActualMaterialCost,
    materialCostVariance = materialCostVariance,
    averageMachineSpeedEfficiency = averageMachineSpeedEfficiency,
    totalDowntimeMinutes = totalDowntimeMinutes,
    isWithinTolerance = isWithinTolerance,
    generatedAt = generatedAt
)

fun ShopFloorTrackingReconciliationResult.toDto(): ShopFloorTrackingReconciliationResponseDto = ShopFloorTrackingReconciliationResponseDto(
    executionJobId = executionJobId,
    tenantId = tenantId,
    isFullyReconciled = isFullyReconciled,
    workOrdersMatched = workOrdersMatched,
    timersConsistent = timersConsistent,
    materialDepletionReconciled = materialDepletionReconciled,
    telemetryLogged = telemetryLogged,
    handoversContinuous = handoversContinuous,
    zeroUnresolvedScrapDiscrepancies = zeroUnresolvedScrapDiscrepancies,
    cryptographicIntegrityPassed = cryptographicIntegrityPassed,
    discrepancies = discrepancies,
    reconciledAt = reconciledAt
)

fun Module17Step07ShopFloorTrackingHandoffContract.toDto(): Module17Step07ShopFloorTrackingHandoffContractDto = Module17Step07ShopFloorTrackingHandoffContractDto(
    contractVersion = contractVersion,
    executionJobId = executionJobId,
    orderId = orderId,
    orderNumber = orderNumber,
    tenantId = tenantId,
    totalStagesCount = totalStagesCount,
    completedStagesCount = completedStagesCount,
    overallYieldPercentage = overallYieldPercentage,
    speedEfficiencyPercentage = speedEfficiencyPercentage,
    totalDowntimeMinutes = totalDowntimeMinutes,
    materialConsumptionsSummary = materialConsumptionsSummary,
    stageHandoversSummary = stageHandoversSummary,
    isFullyReconciled = isFullyReconciled,
    integrityHash = integrityHash,
    generatedAt = generatedAt
)

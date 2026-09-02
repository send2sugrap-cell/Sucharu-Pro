package com.sucharu.sucharupro.domain.model.shopfloortracking

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal

// ============================================================
// ENUMS (Module 17 Step 07)
// ============================================================

enum class OperatorTrackingState(val defaultLabel: String) {
    IDLE("Idle / Ready"),
    SETUP("Machine Setup in Progress"),
    RUNNING("Production Running"),
    PAUSED("Execution Paused"),
    DOWNTIME("Downtime Interruption"),
    COMPLETED("Work Order Completed");

    val isActive: Boolean get() = this == SETUP || this == RUNNING
}

enum class DowntimeCategory(val defaultLabel: String) {
    MECHANICAL_FAULT("Mechanical Fault / Breakdown"),
    ELECTRICAL_FAULT("Electrical / Sensor Issue"),
    SETUP_ADJUSTMENT("Plate / Blanket / Feeder Adjustment"),
    WAITING_FOR_MATERIAL("Waiting for Paper / Ink / Consumables"),
    OPERATOR_BREAK("Operator Shift Handover / Break"),
    CHANGE_OVER("Job / Format Changeover"),
    OTHER("Unscheduled Stoppage")
}

enum class MaterialConsumptionStatus {
    PLANNED,
    RECORDED,
    OVER_CONSUMED,
    REVERSED
}

enum class HandoverStatus {
    PENDING_VERIFICATION,
    ACCEPTED,
    REJECTED_DISCREPANCY
}

enum class ShopFloorTrackingEventType {
    EXECUTION_STARTED,
    EXECUTION_PAUSED,
    EXECUTION_RESUMED,
    DOWNTIME_LOGGED,
    MATERIAL_CONSUMED,
    OUTPUT_RECORDED,
    STAGE_HANDOVER_INITIATED,
    STAGE_HANDOVER_ACCEPTED,
    WORK_ORDER_FINALIZED,
    RECONCILIATION_PERFORMED
}

// ============================================================
// DOMAIN ENTITIES & VALUE OBJECTS
// ============================================================

/**
 * Real-time Operator Time Tracking Record for a Work Order.
 */
data class OperatorTimeTrackingRecord(
    val recordId: String,
    val tenantId: String,
    val workOrderId: String,
    val executionJobId: String,
    val orderId: String,
    val sequenceNumber: Int,
    val stageType: ProductionStageType,
    val machineId: String,
    val machineName: String,
    val operatorId: String,
    val operatorName: String,
    val currentState: OperatorTrackingState = OperatorTrackingState.IDLE,
    val startedAt: Long? = null,
    val setupMinutes: Int = 0,
    val runMinutes: Int = 0,
    val downtimeMinutes: Int = 0,
    val totalActiveMinutes: Int = setupMinutes + runMinutes,
    val goodQuantityProduced: BigDecimal = BigDecimal.ZERO,
    val scrapQuantityProduced: BigDecimal = BigDecimal.ZERO,
    val pausedAt: Long? = null,
    val pauseReason: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Actual Material Consumption & Spoilage Record for a Stage.
 */
data class ProductionMaterialConsumptionRecord(
    val consumptionId: String,
    val tenantId: String,
    val workOrderId: String,
    val executionJobId: String,
    val stageType: ProductionStageType,
    val materialCode: String,
    val materialName: String,
    val unitOfMeasure: String,
    val plannedQuantity: BigDecimal,
    val actualQuantityConsumed: BigDecimal,
    val scrapQuantity: BigDecimal,
    val varianceQuantity: BigDecimal,
    val variancePercentage: BigDecimal,
    val batchLotNumber: String?,
    val status: MaterialConsumptionStatus = MaterialConsumptionStatus.RECORDED,
    val recordedBy: String,
    val recordedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

/**
 * Real-time Machine Telemetry and Speed Log.
 */
data class MachineTelemetryLog(
    val logId: String,
    val tenantId: String,
    val machineId: String,
    val machineName: String,
    val workOrderId: String?,
    val executionJobId: String?,
    val recordedSpeedUnitsPerHour: BigDecimal, // e.g. 6500 sheets/hour
    val ratedSpeedUnitsPerHour: BigDecimal,    // e.g. 8000 sheets/hour
    val speedEfficiencyPercentage: BigDecimal, // (recorded / rated) * 100
    val totalImpressions: Long,
    val currentDowntimeCategory: DowntimeCategory?,
    val downtimeMinutes: Int = 0,
    val temperatureCelsius: BigDecimal? = null,
    val isRunning: Boolean = true,
    val loggedAt: Long = System.currentTimeMillis(),
    val loggedBy: String
)

/**
 * Stage Output Handover Record between sequential stages.
 */
data class StageOutputHandoverRecord(
    val handoverId: String,
    val tenantId: String,
    val executionJobId: String,
    val fromWorkOrderId: String,
    val fromStage: ProductionStageType,
    val toWorkOrderId: String?,
    val toStage: ProductionStageType?,
    val plannedOutputQuantity: BigDecimal,
    val actualGoodQuantity: BigDecimal,
    val scrapQuantity: BigDecimal,
    val yieldPercentage: BigDecimal,
    val handedOverBy: String,
    val handedOverAt: Long = System.currentTimeMillis(),
    val acceptedBy: String? = null,
    val acceptedAt: Long? = null,
    val status: HandoverStatus = HandoverStatus.PENDING_VERIFICATION,
    val discrepancyNotes: String? = null,
    val integrityHash: String
)

/**
 * Unified Execution Variance & Efficiency Summary.
 */
data class ProductionExecutionVarianceSummary(
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
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Append-only Shop Floor Tracking Lifecycle Event.
 */
data class ShopFloorTrackingEvent(
    val eventId: String,
    val tenantId: String,
    val workOrderId: String,
    val executionJobId: String,
    val eventType: ShopFloorTrackingEventType,
    val actor: String,
    val payload: String?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 8-Way Shop-Floor Execution Reconciliation Result.
 */
data class ShopFloorTrackingReconciliationResult(
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
    val discrepancies: List<String> = emptyList(),
    val reconciledAt: Long = System.currentTimeMillis()
)

/**
 * Read-only AI Agent Handoff Contract for Module 17 Step 07.
 */
data class Module17Step07ShopFloorTrackingHandoffContract(
    val contractVersion: String = "1.0.0",
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
    val generatedAt: Long = System.currentTimeMillis()
)

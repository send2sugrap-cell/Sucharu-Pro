package com.sucharu.sucharupro.domain.service.shopfloortracking

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import java.math.BigDecimal

interface ShopFloorTrackingService {

    // 1. Live Operator Time Tracking
    suspend fun startWorkOrderExecution(
        tenantId: String,
        workOrderId: String,
        executionJobId: String,
        orderId: String,
        sequenceNumber: Int,
        stageType: ProductionStageType,
        machineId: String,
        machineName: String,
        operatorId: String,
        operatorName: String,
        isSetup: Boolean = true,
        actor: String
    ): OperatorTimeTrackingRecord

    suspend fun pauseWorkOrderExecution(
        tenantId: String,
        workOrderId: String,
        pauseReason: String,
        downtimeCategory: DowntimeCategory?,
        actor: String
    ): OperatorTimeTrackingRecord

    suspend fun resumeWorkOrderExecution(
        tenantId: String,
        workOrderId: String,
        actor: String
    ): OperatorTimeTrackingRecord

    suspend fun recordWorkOrderOutput(
        tenantId: String,
        workOrderId: String,
        additionalGoodQuantity: BigDecimal,
        additionalScrapQuantity: BigDecimal,
        additionalSetupMinutes: Int,
        additionalRunMinutes: Int,
        additionalDowntimeMinutes: Int,
        isCompleted: Boolean = false,
        actor: String
    ): OperatorTimeTrackingRecord

    suspend fun getOperatorTimeRecord(tenantId: String, workOrderId: String): OperatorTimeTrackingRecord?
    suspend fun listOperatorTimeRecordsByJob(tenantId: String, executionJobId: String): List<OperatorTimeTrackingRecord>

    // 2. Material Consumption
    suspend fun recordMaterialConsumption(
        tenantId: String,
        workOrderId: String,
        executionJobId: String,
        stageType: ProductionStageType,
        materialCode: String,
        materialName: String,
        unitOfMeasure: String,
        plannedQuantity: BigDecimal,
        actualQuantity: BigDecimal,
        scrapQuantity: BigDecimal,
        batchLotNumber: String?,
        notes: String?,
        actor: String
    ): ProductionMaterialConsumptionRecord

    suspend fun listMaterialConsumptionsByJob(tenantId: String, executionJobId: String): List<ProductionMaterialConsumptionRecord>

    // 3. Machine Telemetry
    suspend fun logMachineTelemetry(
        tenantId: String,
        machineId: String,
        machineName: String,
        workOrderId: String?,
        executionJobId: String?,
        recordedSpeedUnitsPerHour: BigDecimal,
        ratedSpeedUnitsPerHour: BigDecimal,
        totalImpressions: Long,
        downtimeCategory: DowntimeCategory?,
        downtimeMinutes: Int,
        temperatureCelsius: BigDecimal?,
        isRunning: Boolean,
        actor: String
    ): MachineTelemetryLog

    suspend fun listMachineTelemetryByJob(tenantId: String, executionJobId: String): List<MachineTelemetryLog>

    // 4. Stage Handover
    suspend fun createStageHandover(
        tenantId: String,
        executionJobId: String,
        fromWorkOrderId: String,
        fromStage: ProductionStageType,
        toWorkOrderId: String?,
        toStage: ProductionStageType?,
        plannedOutputQuantity: BigDecimal,
        actualGoodQuantity: BigDecimal,
        scrapQuantity: BigDecimal,
        discrepancyNotes: String?,
        actor: String
    ): StageOutputHandoverRecord

    suspend fun acceptStageHandover(
        tenantId: String,
        handoverId: String,
        actor: String
    ): StageOutputHandoverRecord

    suspend fun listStageHandoversByJob(tenantId: String, executionJobId: String): List<StageOutputHandoverRecord>

    // 5. Variance & Reconciliation
    suspend fun getExecutionVarianceSummary(
        tenantId: String,
        executionJobId: String,
        plannedDurationMinutes: Int = 480,
        plannedOutputQuantity: BigDecimal = BigDecimal("5000.0000")
    ): ProductionExecutionVarianceSummary

    suspend fun reconcileShopFloorExecution(
        tenantId: String,
        executionJobId: String
    ): ShopFloorTrackingReconciliationResult

    // 6. AI Agent Handoff Contract
    suspend fun getAiHandoffContract(
        tenantId: String,
        executionJobId: String,
        orderId: String = "ORD-001",
        orderNumber: String = "ORD-2026-001"
    ): Module17Step07ShopFloorTrackingHandoffContract
}

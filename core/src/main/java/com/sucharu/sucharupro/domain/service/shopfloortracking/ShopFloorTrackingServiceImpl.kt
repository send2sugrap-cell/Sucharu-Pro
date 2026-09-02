package com.sucharu.sucharupro.domain.service.shopfloortracking

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import com.sucharu.sucharupro.domain.repository.shopfloortracking.ShopFloorTrackingRepository
import java.math.BigDecimal

class ShopFloorTrackingServiceImpl(
    private val repository: ShopFloorTrackingRepository,
    private val materialEngine: ProductionMaterialConsumptionEngine = ProductionMaterialConsumptionEngine(),
    private val telemetryEngine: MachineTelemetryEngine = MachineTelemetryEngine(),
    private val handoverEngine: StageHandoverEngine = StageHandoverEngine(),
    private val varianceEngine: ShopFloorVarianceEngine = ShopFloorVarianceEngine(),
    private val reconciliationEngine: ShopFloorTrackingReconciliationEngine = ShopFloorTrackingReconciliationEngine()
) : ShopFloorTrackingService {

    override suspend fun startWorkOrderExecution(
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
        isSetup: Boolean,
        actor: String
    ): OperatorTimeTrackingRecord {
        val existing = repository.getOperatorTimeRecord(tenantId, workOrderId)
        val record = if (existing != null) {
            existing.copy(
                currentState = if (isSetup) OperatorTrackingState.SETUP else OperatorTrackingState.RUNNING,
                startedAt = existing.startedAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            OperatorTimeTrackingRecord(
                recordId = "TIM-$workOrderId",
                tenantId = tenantId,
                workOrderId = workOrderId,
                executionJobId = executionJobId,
                orderId = orderId,
                sequenceNumber = sequenceNumber,
                stageType = stageType,
                machineId = machineId,
                machineName = machineName,
                operatorId = operatorId,
                operatorName = operatorName,
                currentState = if (isSetup) OperatorTrackingState.SETUP else OperatorTrackingState.RUNNING,
                startedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        repository.saveOperatorTimeRecord(tenantId, record)
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = workOrderId,
                executionJobId = executionJobId,
                eventType = ShopFloorTrackingEventType.EXECUTION_STARTED,
                actor = actor,
                payload = "Started execution in state ${record.currentState}"
            )
        )
        return record
    }

    override suspend fun pauseWorkOrderExecution(
        tenantId: String,
        workOrderId: String,
        pauseReason: String,
        downtimeCategory: DowntimeCategory?,
        actor: String
    ): OperatorTimeTrackingRecord {
        val existing = repository.getOperatorTimeRecord(tenantId, workOrderId)
            ?: throw IllegalArgumentException("Operator time record not found for work order $workOrderId")

        val updated = existing.copy(
            currentState = if (downtimeCategory != null) OperatorTrackingState.DOWNTIME else OperatorTrackingState.PAUSED,
            pausedAt = System.currentTimeMillis(),
            pauseReason = pauseReason,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveOperatorTimeRecord(tenantId, updated)
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = workOrderId,
                executionJobId = existing.executionJobId,
                eventType = if (downtimeCategory != null) ShopFloorTrackingEventType.DOWNTIME_LOGGED else ShopFloorTrackingEventType.EXECUTION_PAUSED,
                actor = actor,
                payload = "Paused reason: $pauseReason (Category: ${downtimeCategory?.name})"
            )
        )
        return updated
    }

    override suspend fun resumeWorkOrderExecution(
        tenantId: String,
        workOrderId: String,
        actor: String
    ): OperatorTimeTrackingRecord {
        val existing = repository.getOperatorTimeRecord(tenantId, workOrderId)
            ?: throw IllegalArgumentException("Operator time record not found for work order $workOrderId")

        val updated = existing.copy(
            currentState = OperatorTrackingState.RUNNING,
            pausedAt = null,
            pauseReason = null,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveOperatorTimeRecord(tenantId, updated)
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = workOrderId,
                executionJobId = existing.executionJobId,
                eventType = ShopFloorTrackingEventType.EXECUTION_RESUMED,
                actor = actor,
                payload = "Resumed execution"
            )
        )
        return updated
    }

    override suspend fun recordWorkOrderOutput(
        tenantId: String,
        workOrderId: String,
        additionalGoodQuantity: BigDecimal,
        additionalScrapQuantity: BigDecimal,
        additionalSetupMinutes: Int,
        additionalRunMinutes: Int,
        additionalDowntimeMinutes: Int,
        isCompleted: Boolean,
        actor: String
    ): OperatorTimeTrackingRecord {
        val existing = repository.getOperatorTimeRecord(tenantId, workOrderId)
            ?: throw IllegalArgumentException("Operator time record not found for work order $workOrderId")

        val newGood = existing.goodQuantityProduced.add(additionalGoodQuantity)
        val newScrap = existing.scrapQuantityProduced.add(additionalScrapQuantity)
        val newSetup = existing.setupMinutes + additionalSetupMinutes
        val newRun = existing.runMinutes + additionalRunMinutes
        val newDown = existing.downtimeMinutes + additionalDowntimeMinutes

        val updated = existing.copy(
            currentState = if (isCompleted) OperatorTrackingState.COMPLETED else existing.currentState,
            setupMinutes = newSetup,
            runMinutes = newRun,
            downtimeMinutes = newDown,
            goodQuantityProduced = newGood,
            scrapQuantityProduced = newScrap,
            completedAt = if (isCompleted) System.currentTimeMillis() else existing.completedAt,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveOperatorTimeRecord(tenantId, updated)
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = workOrderId,
                executionJobId = existing.executionJobId,
                eventType = if (isCompleted) ShopFloorTrackingEventType.WORK_ORDER_FINALIZED else ShopFloorTrackingEventType.OUTPUT_RECORDED,
                actor = actor,
                payload = "Output: +$additionalGoodQuantity good, +$additionalScrapQuantity scrap. Completed: $isCompleted"
            )
        )
        return updated
    }

    override suspend fun getOperatorTimeRecord(tenantId: String, workOrderId: String): OperatorTimeTrackingRecord? {
        return repository.getOperatorTimeRecord(tenantId, workOrderId)
    }

    override suspend fun listOperatorTimeRecordsByJob(tenantId: String, executionJobId: String): List<OperatorTimeTrackingRecord> {
        return repository.listOperatorTimeRecordsByJob(tenantId, executionJobId)
    }

    override suspend fun recordMaterialConsumption(
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
    ): ProductionMaterialConsumptionRecord {
        val record = materialEngine.recordConsumption(
            tenantId = tenantId,
            workOrderId = workOrderId,
            executionJobId = executionJobId,
            stageType = stageType,
            materialCode = materialCode,
            materialName = materialName,
            unitOfMeasure = unitOfMeasure,
            plannedQuantity = plannedQuantity,
            actualQuantity = actualQuantity,
            scrapQuantity = scrapQuantity,
            batchLotNumber = batchLotNumber,
            recordedBy = actor,
            notes = notes
        )
        repository.saveMaterialConsumptionRecord(tenantId, record)
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = workOrderId,
                executionJobId = executionJobId,
                eventType = ShopFloorTrackingEventType.MATERIAL_CONSUMED,
                actor = actor,
                payload = "Consumed $actualQuantity $unitOfMeasure of $materialName"
            )
        )
        return record
    }

    override suspend fun listMaterialConsumptionsByJob(tenantId: String, executionJobId: String): List<ProductionMaterialConsumptionRecord> {
        return repository.listMaterialConsumptionsByJob(tenantId, executionJobId)
    }

    override suspend fun logMachineTelemetry(
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
    ): MachineTelemetryLog {
        val log = telemetryEngine.logTelemetry(
            tenantId = tenantId,
            machineId = machineId,
            machineName = machineName,
            workOrderId = workOrderId,
            executionJobId = executionJobId,
            recordedSpeed = recordedSpeedUnitsPerHour,
            ratedSpeed = ratedSpeedUnitsPerHour,
            totalImpressions = totalImpressions,
            downtimeCategory = downtimeCategory,
            downtimeMinutes = downtimeMinutes,
            temperatureCelsius = temperatureCelsius,
            isRunning = isRunning,
            loggedBy = actor
        )
        repository.saveMachineTelemetryLog(tenantId, log)
        return log
    }

    override suspend fun listMachineTelemetryByJob(tenantId: String, executionJobId: String): List<MachineTelemetryLog> {
        return repository.listMachineTelemetryLogsByJob(tenantId, executionJobId)
    }

    override suspend fun createStageHandover(
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
    ): StageOutputHandoverRecord {
        val record = handoverEngine.createHandover(
            tenantId = tenantId,
            executionJobId = executionJobId,
            fromWorkOrderId = fromWorkOrderId,
            fromStage = fromStage,
            toWorkOrderId = toWorkOrderId,
            toStage = toStage,
            plannedOutputQuantity = plannedOutputQuantity,
            actualGoodQuantity = actualGoodQuantity,
            scrapQuantity = scrapQuantity,
            handedOverBy = actor,
            discrepancyNotes = discrepancyNotes
        )
        repository.saveStageHandoverRecord(tenantId, record)
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = fromWorkOrderId,
                executionJobId = executionJobId,
                eventType = ShopFloorTrackingEventType.STAGE_HANDOVER_INITIATED,
                actor = actor,
                payload = "Handover from ${fromStage.name} to ${toStage?.name ?: "END"}: $actualGoodQuantity good pieces"
            )
        )
        return record
    }

    override suspend fun acceptStageHandover(
        tenantId: String,
        handoverId: String,
        actor: String
    ): StageOutputHandoverRecord {
        val existing = repository.getStageHandoverRecord(tenantId, handoverId)
            ?: throw IllegalArgumentException("Stage handover $handoverId not found")

        val accepted = handoverEngine.acceptHandover(existing, actor)
        repository.saveStageHandoverRecord(tenantId, accepted)
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = accepted.fromWorkOrderId,
                executionJobId = accepted.executionJobId,
                eventType = ShopFloorTrackingEventType.STAGE_HANDOVER_ACCEPTED,
                actor = actor,
                payload = "Stage handover $handoverId accepted by $actor"
            )
        )
        return accepted
    }

    override suspend fun listStageHandoversByJob(tenantId: String, executionJobId: String): List<StageOutputHandoverRecord> {
        return repository.listStageHandoversByJob(tenantId, executionJobId)
    }

    override suspend fun getExecutionVarianceSummary(
        tenantId: String,
        executionJobId: String,
        plannedDurationMinutes: Int,
        plannedOutputQuantity: BigDecimal
    ): ProductionExecutionVarianceSummary {
        val times = repository.listOperatorTimeRecordsByJob(tenantId, executionJobId)
        val materials = repository.listMaterialConsumptionsByJob(tenantId, executionJobId)
        val telemetry = repository.listMachineTelemetryLogsByJob(tenantId, executionJobId)

        return varianceEngine.generateVarianceSummary(
            executionJobId = executionJobId,
            tenantId = tenantId,
            plannedDurationMinutes = plannedDurationMinutes,
            timeRecords = times,
            materialRecords = materials,
            telemetryLogs = telemetry,
            plannedOutputQuantity = plannedOutputQuantity
        )
    }

    override suspend fun reconcileShopFloorExecution(
        tenantId: String,
        executionJobId: String
    ): ShopFloorTrackingReconciliationResult {
        val times = repository.listOperatorTimeRecordsByJob(tenantId, executionJobId)
        val materials = repository.listMaterialConsumptionsByJob(tenantId, executionJobId)
        val telemetry = repository.listMachineTelemetryLogsByJob(tenantId, executionJobId)
        val handovers = repository.listStageHandoversByJob(tenantId, executionJobId)

        val result = reconciliationEngine.reconcile(
            executionJobId = executionJobId,
            tenantId = tenantId,
            timeRecords = times,
            materialRecords = materials,
            telemetryLogs = telemetry,
            handovers = handovers
        )
        repository.saveShopFloorEvent(
            tenantId,
            ShopFloorTrackingEvent(
                eventId = "EVT-${System.currentTimeMillis()}",
                tenantId = tenantId,
                workOrderId = times.firstOrNull()?.workOrderId ?: "JOB-ROOT",
                executionJobId = executionJobId,
                eventType = ShopFloorTrackingEventType.RECONCILIATION_PERFORMED,
                actor = "system-reconciler",
                payload = "Reconciliation completed. Fully reconciled: ${result.isFullyReconciled}"
            )
        )
        return result
    }

    override suspend fun getAiHandoffContract(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        orderNumber: String
    ): Module17Step07ShopFloorTrackingHandoffContract {
        val times = repository.listOperatorTimeRecordsByJob(tenantId, executionJobId)
        val materials = repository.listMaterialConsumptionsByJob(tenantId, executionJobId)
        val telemetry = repository.listMachineTelemetryLogsByJob(tenantId, executionJobId)
        val handovers = repository.listStageHandoversByJob(tenantId, executionJobId)
        val recon = reconcileShopFloorExecution(tenantId, executionJobId)
        val variance = getExecutionVarianceSummary(tenantId, executionJobId)

        val hashPayload = "$tenantId|$executionJobId|$orderId|${variance.overallYieldPercentage}|${variance.averageMachineSpeedEfficiency}|${recon.isFullyReconciled}"
        val integrityHash = ProductionTrackingMathUtils.sha256Hex(hashPayload)

        return Module17Step07ShopFloorTrackingHandoffContract(
            contractVersion = "1.0.0",
            executionJobId = executionJobId,
            orderId = orderId,
            orderNumber = orderNumber,
            tenantId = tenantId,
            totalStagesCount = times.size,
            completedStagesCount = times.count { it.currentState == OperatorTrackingState.COMPLETED },
            overallYieldPercentage = variance.overallYieldPercentage,
            speedEfficiencyPercentage = variance.averageMachineSpeedEfficiency,
            totalDowntimeMinutes = variance.totalDowntimeMinutes,
            materialConsumptionsSummary = materials.map { "${it.materialName}: ${it.actualQuantityConsumed} ${it.unitOfMeasure} (Scrap: ${it.scrapQuantity})" },
            stageHandoversSummary = handovers.map { "${it.fromStage.name} -> ${it.toStage?.name ?: "COMPLETED"}: ${it.actualGoodQuantity} pcs (Yield: ${it.yieldPercentage}%)" },
            isFullyReconciled = recon.isFullyReconciled,
            integrityHash = integrityHash,
            generatedAt = System.currentTimeMillis()
        )
    }
}

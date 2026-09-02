package com.sucharu.sucharupro.data.datasource.shopfloortracking

import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import java.util.concurrent.ConcurrentHashMap

class FakeShopFloorTrackingDataSource : ShopFloorTrackingDataSource {

    private val timeRecords = ConcurrentHashMap<String, OperatorTimeTrackingRecord>() // key: tenantId:workOrderId
    private val materialRecords = ConcurrentHashMap<String, ProductionMaterialConsumptionRecord>() // key: tenantId:consumptionId
    private val telemetryLogs = ConcurrentHashMap<String, MachineTelemetryLog>() // key: tenantId:logId
    private val handovers = ConcurrentHashMap<String, StageOutputHandoverRecord>() // key: tenantId:handoverId
    private val events = ConcurrentHashMap<String, ShopFloorTrackingEvent>() // key: tenantId:eventId

    override suspend fun saveOperatorTimeRecord(tenantId: String, record: OperatorTimeTrackingRecord) {
        timeRecords["$tenantId:${record.workOrderId}"] = record
    }

    override suspend fun getOperatorTimeRecord(tenantId: String, workOrderId: String): OperatorTimeTrackingRecord? {
        return timeRecords["$tenantId:$workOrderId"]
    }

    override suspend fun listOperatorTimeRecordsByJob(tenantId: String, executionJobId: String): List<OperatorTimeTrackingRecord> {
        return timeRecords.values.filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedBy { it.sequenceNumber }
    }

    override suspend fun listOperatorTimeRecordsByOperator(tenantId: String, operatorId: String): List<OperatorTimeTrackingRecord> {
        return timeRecords.values.filter { it.tenantId == tenantId && it.operatorId == operatorId }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun saveMaterialConsumptionRecord(tenantId: String, record: ProductionMaterialConsumptionRecord) {
        materialRecords["$tenantId:${record.consumptionId}"] = record
    }

    override suspend fun listMaterialConsumptionsByJob(tenantId: String, executionJobId: String): List<ProductionMaterialConsumptionRecord> {
        return materialRecords.values.filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedBy { it.recordedAt }
    }

    override suspend fun listMaterialConsumptionsByWorkOrder(tenantId: String, workOrderId: String): List<ProductionMaterialConsumptionRecord> {
        return materialRecords.values.filter { it.tenantId == tenantId && it.workOrderId == workOrderId }
            .sortedBy { it.recordedAt }
    }

    override suspend fun saveMachineTelemetryLog(tenantId: String, log: MachineTelemetryLog) {
        telemetryLogs["$tenantId:${log.logId}"] = log
    }

    override suspend fun listMachineTelemetryLogsByMachine(tenantId: String, machineId: String, limit: Int): List<MachineTelemetryLog> {
        return telemetryLogs.values.filter { it.tenantId == tenantId && it.machineId == machineId }
            .sortedByDescending { it.loggedAt }
            .take(limit)
    }

    override suspend fun listMachineTelemetryLogsByJob(tenantId: String, executionJobId: String): List<MachineTelemetryLog> {
        return telemetryLogs.values.filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedByDescending { it.loggedAt }
    }

    override suspend fun saveStageHandoverRecord(tenantId: String, handover: StageOutputHandoverRecord) {
        handovers["$tenantId:${handover.handoverId}"] = handover
    }

    override suspend fun getStageHandoverRecord(tenantId: String, handoverId: String): StageOutputHandoverRecord? {
        return handovers["$tenantId:$handoverId"]
    }

    override suspend fun listStageHandoversByJob(tenantId: String, executionJobId: String): List<StageOutputHandoverRecord> {
        return handovers.values.filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedBy { it.handedOverAt }
    }

    override suspend fun saveShopFloorEvent(tenantId: String, event: ShopFloorTrackingEvent) {
        events["$tenantId:${event.eventId}"] = event
    }

    override suspend fun listShopFloorEventsByJob(tenantId: String, executionJobId: String): List<ShopFloorTrackingEvent> {
        return events.values.filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedBy { it.timestamp }
    }
}

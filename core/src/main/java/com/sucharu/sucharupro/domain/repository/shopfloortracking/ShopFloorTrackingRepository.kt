package com.sucharu.sucharupro.domain.repository.shopfloortracking

import com.sucharu.sucharupro.domain.model.shopfloortracking.*

interface ShopFloorTrackingRepository {

    suspend fun saveOperatorTimeRecord(tenantId: String, record: OperatorTimeTrackingRecord)
    suspend fun getOperatorTimeRecord(tenantId: String, workOrderId: String): OperatorTimeTrackingRecord?
    suspend fun listOperatorTimeRecordsByJob(tenantId: String, executionJobId: String): List<OperatorTimeTrackingRecord>
    suspend fun listOperatorTimeRecordsByOperator(tenantId: String, operatorId: String): List<OperatorTimeTrackingRecord>

    suspend fun saveMaterialConsumptionRecord(tenantId: String, record: ProductionMaterialConsumptionRecord)
    suspend fun listMaterialConsumptionsByJob(tenantId: String, executionJobId: String): List<ProductionMaterialConsumptionRecord>
    suspend fun listMaterialConsumptionsByWorkOrder(tenantId: String, workOrderId: String): List<ProductionMaterialConsumptionRecord>

    suspend fun saveMachineTelemetryLog(tenantId: String, log: MachineTelemetryLog)
    suspend fun listMachineTelemetryLogsByMachine(tenantId: String, machineId: String, limit: Int = 50): List<MachineTelemetryLog>
    suspend fun listMachineTelemetryLogsByJob(tenantId: String, executionJobId: String): List<MachineTelemetryLog>

    suspend fun saveStageHandoverRecord(tenantId: String, handover: StageOutputHandoverRecord)
    suspend fun getStageHandoverRecord(tenantId: String, handoverId: String): StageOutputHandoverRecord?
    suspend fun listStageHandoversByJob(tenantId: String, executionJobId: String): List<StageOutputHandoverRecord>

    suspend fun saveShopFloorEvent(tenantId: String, event: ShopFloorTrackingEvent)
    suspend fun listShopFloorEventsByJob(tenantId: String, executionJobId: String): List<ShopFloorTrackingEvent>
}

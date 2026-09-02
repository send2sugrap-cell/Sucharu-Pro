package com.sucharu.sucharupro.data.repository.shopfloortracking

import com.sucharu.sucharupro.data.datasource.shopfloortracking.ShopFloorTrackingDataSource
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import com.sucharu.sucharupro.domain.repository.shopfloortracking.ShopFloorTrackingRepository

class ShopFloorTrackingRepositoryImpl(
    private val dataSource: ShopFloorTrackingDataSource
) : ShopFloorTrackingRepository {

    override suspend fun saveOperatorTimeRecord(tenantId: String, record: OperatorTimeTrackingRecord) {
        dataSource.saveOperatorTimeRecord(tenantId, record)
    }

    override suspend fun getOperatorTimeRecord(tenantId: String, workOrderId: String): OperatorTimeTrackingRecord? {
        return dataSource.getOperatorTimeRecord(tenantId, workOrderId)
    }

    override suspend fun listOperatorTimeRecordsByJob(tenantId: String, executionJobId: String): List<OperatorTimeTrackingRecord> {
        return dataSource.listOperatorTimeRecordsByJob(tenantId, executionJobId)
    }

    override suspend fun listOperatorTimeRecordsByOperator(tenantId: String, operatorId: String): List<OperatorTimeTrackingRecord> {
        return dataSource.listOperatorTimeRecordsByOperator(tenantId, operatorId)
    }

    override suspend fun saveMaterialConsumptionRecord(tenantId: String, record: ProductionMaterialConsumptionRecord) {
        dataSource.saveMaterialConsumptionRecord(tenantId, record)
    }

    override suspend fun listMaterialConsumptionsByJob(tenantId: String, executionJobId: String): List<ProductionMaterialConsumptionRecord> {
        return dataSource.listMaterialConsumptionsByJob(tenantId, executionJobId)
    }

    override suspend fun listMaterialConsumptionsByWorkOrder(tenantId: String, workOrderId: String): List<ProductionMaterialConsumptionRecord> {
        return dataSource.listMaterialConsumptionsByWorkOrder(tenantId, workOrderId)
    }

    override suspend fun saveMachineTelemetryLog(tenantId: String, log: MachineTelemetryLog) {
        dataSource.saveMachineTelemetryLog(tenantId, log)
    }

    override suspend fun listMachineTelemetryLogsByMachine(tenantId: String, machineId: String, limit: Int): List<MachineTelemetryLog> {
        return dataSource.listMachineTelemetryLogsByMachine(tenantId, machineId, limit)
    }

    override suspend fun listMachineTelemetryLogsByJob(tenantId: String, executionJobId: String): List<MachineTelemetryLog> {
        return dataSource.listMachineTelemetryLogsByJob(tenantId, executionJobId)
    }

    override suspend fun saveStageHandoverRecord(tenantId: String, handover: StageOutputHandoverRecord) {
        dataSource.saveStageHandoverRecord(tenantId, handover)
    }

    override suspend fun getStageHandoverRecord(tenantId: String, handoverId: String): StageOutputHandoverRecord? {
        return dataSource.getStageHandoverRecord(tenantId, handoverId)
    }

    override suspend fun listStageHandoversByJob(tenantId: String, executionJobId: String): List<StageOutputHandoverRecord> {
        return dataSource.listStageHandoversByJob(tenantId, executionJobId)
    }

    override suspend fun saveShopFloorEvent(tenantId: String, event: ShopFloorTrackingEvent) {
        dataSource.saveShopFloorEvent(tenantId, event)
    }

    override suspend fun listShopFloorEventsByJob(tenantId: String, executionJobId: String): List<ShopFloorTrackingEvent> {
        return dataSource.listShopFloorEventsByJob(tenantId, executionJobId)
    }
}

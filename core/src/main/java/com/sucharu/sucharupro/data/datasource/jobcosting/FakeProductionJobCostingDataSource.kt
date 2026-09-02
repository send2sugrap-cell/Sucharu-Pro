package com.sucharu.sucharupro.data.datasource.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.*
import java.util.concurrent.ConcurrentHashMap

class FakeProductionJobCostingDataSource : ProductionJobCostingDataSource {

    private val costRecords = ConcurrentHashMap<String, ProductionActualJobCostRecord>()
    private val varianceRecords = ConcurrentHashMap<String, ProductionJobCostVarianceSummary>()
    private val reconciliationRecords = ConcurrentHashMap<String, ProductionJobCostingReconciliationResult>()
    private val events = ConcurrentHashMap<String, MutableList<ProductionJobCostingEvent>>()

    override suspend fun saveActualJobCost(tenantId: String, costRecord: ProductionActualJobCostRecord) {
        costRecords["$tenantId:${costRecord.costRecordId}"] = costRecord
        costRecords["$tenantId:job:${costRecord.executionJobId}"] = costRecord
    }

    override suspend fun getActualJobCost(tenantId: String, costRecordId: String): ProductionActualJobCostRecord? {
        return costRecords["$tenantId:$costRecordId"]
    }

    override suspend fun getActualJobCostByJob(tenantId: String, executionJobId: String): ProductionActualJobCostRecord? {
        return costRecords["$tenantId:job:$executionJobId"]
    }

    override suspend fun saveVarianceSummary(tenantId: String, variance: ProductionJobCostVarianceSummary) {
        varianceRecords["$tenantId:${variance.executionJobId}"] = variance
    }

    override suspend fun getVarianceSummaryByJob(tenantId: String, executionJobId: String): ProductionJobCostVarianceSummary? {
        return varianceRecords["$tenantId:$executionJobId"]
    }

    override suspend fun saveReconciliationResult(tenantId: String, reconciliation: ProductionJobCostingReconciliationResult) {
        reconciliationRecords["$tenantId:${reconciliation.executionJobId}"] = reconciliation
    }

    override suspend fun getReconciliationResultByJob(tenantId: String, executionJobId: String): ProductionJobCostingReconciliationResult? {
        return reconciliationRecords["$tenantId:$executionJobId"]
    }

    override suspend fun saveEvent(tenantId: String, event: ProductionJobCostingEvent) {
        val list = events.computeIfAbsent("$tenantId:${event.executionJobId}") { mutableListOf() }
        synchronized(list) {
            list.add(event)
        }
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobCostingEvent> {
        return events["$tenantId:$executionJobId"]?.toList() ?: emptyList()
    }
}

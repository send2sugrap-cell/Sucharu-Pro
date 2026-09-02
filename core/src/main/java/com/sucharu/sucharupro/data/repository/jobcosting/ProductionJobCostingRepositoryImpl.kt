package com.sucharu.sucharupro.data.repository.jobcosting

import com.sucharu.sucharupro.data.datasource.jobcosting.ProductionJobCostingDataSource
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.repository.jobcosting.ProductionJobCostingRepository

class ProductionJobCostingRepositoryImpl(
    private val dataSource: ProductionJobCostingDataSource
) : ProductionJobCostingRepository {

    override suspend fun saveActualJobCost(tenantId: String, costRecord: ProductionActualJobCostRecord) {
        dataSource.saveActualJobCost(tenantId, costRecord)
    }

    override suspend fun getActualJobCost(tenantId: String, costRecordId: String): ProductionActualJobCostRecord? {
        return dataSource.getActualJobCost(tenantId, costRecordId)
    }

    override suspend fun getActualJobCostByJob(tenantId: String, executionJobId: String): ProductionActualJobCostRecord? {
        return dataSource.getActualJobCostByJob(tenantId, executionJobId)
    }

    override suspend fun saveVarianceSummary(tenantId: String, variance: ProductionJobCostVarianceSummary) {
        dataSource.saveVarianceSummary(tenantId, variance)
    }

    override suspend fun getVarianceSummaryByJob(tenantId: String, executionJobId: String): ProductionJobCostVarianceSummary? {
        return dataSource.getVarianceSummaryByJob(tenantId, executionJobId)
    }

    override suspend fun saveReconciliationResult(tenantId: String, reconciliation: ProductionJobCostingReconciliationResult) {
        dataSource.saveReconciliationResult(tenantId, reconciliation)
    }

    override suspend fun getReconciliationResultByJob(tenantId: String, executionJobId: String): ProductionJobCostingReconciliationResult? {
        return dataSource.getReconciliationResultByJob(tenantId, executionJobId)
    }

    override suspend fun saveEvent(tenantId: String, event: ProductionJobCostingEvent) {
        dataSource.saveEvent(tenantId, event)
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobCostingEvent> {
        return dataSource.listEventsByJob(tenantId, executionJobId)
    }
}

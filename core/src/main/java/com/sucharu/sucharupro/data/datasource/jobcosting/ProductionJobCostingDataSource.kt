package com.sucharu.sucharupro.data.datasource.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.*

interface ProductionJobCostingDataSource {
    suspend fun saveActualJobCost(tenantId: String, costRecord: ProductionActualJobCostRecord)
    suspend fun getActualJobCost(tenantId: String, costRecordId: String): ProductionActualJobCostRecord?
    suspend fun getActualJobCostByJob(tenantId: String, executionJobId: String): ProductionActualJobCostRecord?

    suspend fun saveVarianceSummary(tenantId: String, variance: ProductionJobCostVarianceSummary)
    suspend fun getVarianceSummaryByJob(tenantId: String, executionJobId: String): ProductionJobCostVarianceSummary?

    suspend fun saveReconciliationResult(tenantId: String, reconciliation: ProductionJobCostingReconciliationResult)
    suspend fun getReconciliationResultByJob(tenantId: String, executionJobId: String): ProductionJobCostingReconciliationResult?

    suspend fun saveEvent(tenantId: String, event: ProductionJobCostingEvent)
    suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobCostingEvent>
}

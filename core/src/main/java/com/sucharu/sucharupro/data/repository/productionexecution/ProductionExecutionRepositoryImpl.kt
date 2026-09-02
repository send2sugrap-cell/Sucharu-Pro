package com.sucharu.sucharupro.data.repository.productionexecution

import com.sucharu.sucharupro.data.datasource.productionexecution.ProductionExecutionDataSource
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.repository.productionexecution.ProductionExecutionRepository

class ProductionExecutionRepositoryImpl(
    private val dataSource: ProductionExecutionDataSource
) : ProductionExecutionRepository {

    override suspend fun saveJobExecution(
        job: ProductionJobExecution,
        idempotencyKey: String?
    ): ProductionJobExecution = dataSource.saveJobExecution(job, idempotencyKey)

    override suspend fun getJobExecutionById(
        tenantId: String,
        executionJobId: String
    ): ProductionJobExecution? = dataSource.getJobExecutionById(tenantId, executionJobId)

    override suspend fun getJobExecutionByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionJobExecution? = dataSource.getJobExecutionByIdempotencyKey(tenantId, idempotencyKey)

    override suspend fun listJobExecutionsByOrder(
        tenantId: String,
        orderId: String
    ): List<ProductionJobExecution> = dataSource.listJobExecutionsByOrder(tenantId, orderId)

    override suspend fun listJobExecutions(
        tenantId: String,
        limit: Int
    ): List<ProductionJobExecution> = dataSource.listJobExecutions(tenantId, limit)

    override suspend fun updateWorkOrder(
        workOrder: ProductionWorkOrder
    ): ProductionWorkOrder = dataSource.updateWorkOrder(workOrder)

    override suspend fun listWorkOrders(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWorkOrder> = dataSource.listWorkOrders(tenantId, executionJobId)

    override suspend fun saveActual(
        actual: ProductionExecutionActual
    ): ProductionExecutionActual = dataSource.saveActual(actual)

    override suspend fun listActuals(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionActual> = dataSource.listActuals(tenantId, executionJobId)

    override suspend fun saveHold(
        hold: ProductionHold
    ): ProductionHold = dataSource.saveHold(hold)

    override suspend fun saveWastage(
        wastage: ProductionWastageRecord
    ): ProductionWastageRecord = dataSource.saveWastage(wastage)

    override suspend fun listWastages(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWastageRecord> = dataSource.listWastages(tenantId, executionJobId)

    override suspend fun saveRework(
        rework: ProductionReworkRecord
    ): ProductionReworkRecord = dataSource.saveRework(rework)

    override suspend fun listReworks(
        tenantId: String,
        executionJobId: String
    ): List<ProductionReworkRecord> = dataSource.listReworks(tenantId, executionJobId)

    override suspend fun saveExecutionEvent(
        event: ProductionExecutionEvent
    ): ProductionExecutionEvent = dataSource.saveExecutionEvent(event)

    override suspend fun listExecutionEvents(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionEvent> = dataSource.listExecutionEvents(tenantId, executionJobId)
}

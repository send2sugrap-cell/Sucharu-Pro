package com.sucharu.sucharupro.domain.repository.productionexecution

import com.sucharu.sucharupro.domain.model.productionexecution.*

interface ProductionExecutionRepository {

    suspend fun saveJobExecution(
        job: ProductionJobExecution,
        idempotencyKey: String? = null
    ): ProductionJobExecution

    suspend fun getJobExecutionById(
        tenantId: String,
        executionJobId: String
    ): ProductionJobExecution?

    suspend fun getJobExecutionByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionJobExecution?

    suspend fun listJobExecutionsByOrder(
        tenantId: String,
        orderId: String
    ): List<ProductionJobExecution>

    suspend fun listJobExecutions(
        tenantId: String,
        limit: Int = 50
    ): List<ProductionJobExecution>

    suspend fun updateWorkOrder(
        workOrder: ProductionWorkOrder
    ): ProductionWorkOrder

    suspend fun listWorkOrders(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWorkOrder>

    suspend fun saveActual(
        actual: ProductionExecutionActual
    ): ProductionExecutionActual

    suspend fun listActuals(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionActual>

    suspend fun saveHold(
        hold: ProductionHold
    ): ProductionHold

    suspend fun saveWastage(
        wastage: ProductionWastageRecord
    ): ProductionWastageRecord

    suspend fun listWastages(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWastageRecord>

    suspend fun saveRework(
        rework: ProductionReworkRecord
    ): ProductionReworkRecord

    suspend fun listReworks(
        tenantId: String,
        executionJobId: String
    ): List<ProductionReworkRecord>

    suspend fun saveExecutionEvent(
        event: ProductionExecutionEvent
    ): ProductionExecutionEvent

    suspend fun listExecutionEvents(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionEvent>
}

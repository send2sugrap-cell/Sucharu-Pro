package com.sucharu.sucharupro.data.datasource.productionexecution

import com.sucharu.sucharupro.domain.model.productionexecution.*
import java.util.concurrent.ConcurrentHashMap

class FakeProductionExecutionDataSource : ProductionExecutionDataSource {

    private val jobs = ConcurrentHashMap<String, ProductionJobExecution>()
    private val idempotencyMap = ConcurrentHashMap<String, String>() // (tenant:key) -> jobId
    private val workOrders = ConcurrentHashMap<String, ProductionWorkOrder>()
    private val actuals = ConcurrentHashMap<String, ProductionExecutionActual>()
    private val holds = ConcurrentHashMap<String, ProductionHold>()
    private val wastages = ConcurrentHashMap<String, ProductionWastageRecord>()
    private val reworks = ConcurrentHashMap<String, ProductionReworkRecord>()
    private val events = ConcurrentHashMap<String, ProductionExecutionEvent>()

    override suspend fun saveJobExecution(
        job: ProductionJobExecution,
        idempotencyKey: String?
    ): ProductionJobExecution {
        jobs[job.executionJobId] = job
        if (idempotencyKey != null) {
            idempotencyMap["${job.tenantId}:$idempotencyKey"] = job.executionJobId
        }
        job.workOrders.forEach { wo ->
            workOrders[wo.workOrderId] = wo
        }
        return job
    }

    override suspend fun getJobExecutionById(
        tenantId: String,
        executionJobId: String
    ): ProductionJobExecution? {
        val job = jobs[executionJobId] ?: return null
        if (job.tenantId != tenantId) return null
        val wos = workOrders.values.filter { it.executionJobId == executionJobId }.sortedBy { it.sequenceNumber }
        val currentHold = holds.values.find { it.executionJobId == executionJobId && !it.isResolved }
        return job.copy(workOrders = wos, currentHold = currentHold)
    }

    override suspend fun getJobExecutionByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionJobExecution? {
        val jobId = idempotencyMap["$tenantId:$idempotencyKey"] ?: return null
        return getJobExecutionById(tenantId, jobId)
    }

    override suspend fun listJobExecutionsByOrder(
        tenantId: String,
        orderId: String
    ): List<ProductionJobExecution> {
        return jobs.values
            .filter { it.tenantId == tenantId && it.orderId == orderId }
            .map { job ->
                val wos = workOrders.values.filter { it.executionJobId == job.executionJobId }.sortedBy { it.sequenceNumber }
                val currentHold = holds.values.find { it.executionJobId == job.executionJobId && !it.isResolved }
                job.copy(workOrders = wos, currentHold = currentHold)
            }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun listJobExecutions(
        tenantId: String,
        limit: Int
    ): List<ProductionJobExecution> {
        return jobs.values
            .filter { it.tenantId == tenantId }
            .map { job ->
                val wos = workOrders.values.filter { it.executionJobId == job.executionJobId }.sortedBy { it.sequenceNumber }
                val currentHold = holds.values.find { it.executionJobId == job.executionJobId && !it.isResolved }
                job.copy(workOrders = wos, currentHold = currentHold)
            }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    override suspend fun updateWorkOrder(
        workOrder: ProductionWorkOrder
    ): ProductionWorkOrder {
        workOrders[workOrder.workOrderId] = workOrder
        return workOrder
    }

    override suspend fun listWorkOrders(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWorkOrder> {
        return workOrders.values
            .filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedBy { it.sequenceNumber }
    }

    override suspend fun saveActual(
        actual: ProductionExecutionActual
    ): ProductionExecutionActual {
        actuals[actual.actualId] = actual
        return actual
    }

    override suspend fun listActuals(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionActual> {
        return actuals.values
            .filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedByDescending { it.startedAt }
    }

    override suspend fun saveHold(
        hold: ProductionHold
    ): ProductionHold {
        holds[hold.holdId] = hold
        return hold
    }

    override suspend fun saveWastage(
        wastage: ProductionWastageRecord
    ): ProductionWastageRecord {
        wastages[wastage.wastageId] = wastage
        return wastage
    }

    override suspend fun listWastages(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWastageRecord> {
        return wastages.values
            .filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedByDescending { it.recordedAt }
    }

    override suspend fun saveRework(
        rework: ProductionReworkRecord
    ): ProductionReworkRecord {
        reworks[rework.reworkId] = rework
        return rework
    }

    override suspend fun listReworks(
        tenantId: String,
        executionJobId: String
    ): List<ProductionReworkRecord> {
        return reworks.values
            .filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedByDescending { it.requestedAt }
    }

    override suspend fun saveExecutionEvent(
        event: ProductionExecutionEvent
    ): ProductionExecutionEvent {
        events[event.eventId] = event
        return event
    }

    override suspend fun listExecutionEvents(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionEvent> {
        return events.values
            .filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedBy { it.performedAt }
    }
}

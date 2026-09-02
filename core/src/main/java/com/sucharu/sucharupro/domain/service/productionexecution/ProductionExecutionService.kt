package com.sucharu.sucharupro.domain.service.productionexecution

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.productionexecution.*
import java.math.BigDecimal

interface ProductionExecutionService {

    suspend fun evaluateJobEligibility(
        tenantId: String,
        orderId: String
    ): DomainResult<List<ProductionExecutionDiagnostic>>

    suspend fun createJobExecution(
        tenantId: String,
        orderId: String,
        requestedBy: String,
        idempotencyKey: String? = null
    ): DomainResult<ProductionJobExecution>

    suspend fun getJobExecution(
        tenantId: String,
        executionJobId: String
    ): DomainResult<ProductionJobExecution?>

    suspend fun listJobExecutionsByOrder(
        tenantId: String,
        orderId: String
    ): DomainResult<List<ProductionJobExecution>>

    suspend fun listJobExecutions(
        tenantId: String,
        limit: Int = 50
    ): DomainResult<List<ProductionJobExecution>>

    suspend fun releaseJob(
        tenantId: String,
        executionJobId: String,
        releasedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun scheduleJob(
        tenantId: String,
        executionJobId: String,
        scheduledBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun startStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        operatorId: String?,
        machineId: String?,
        startedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun pauseStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        reason: String?,
        pausedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun resumeStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        resumedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun completeStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        goodQuantity: BigDecimal,
        scrapQuantity: BigDecimal,
        notes: String?,
        completedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun assignMachine(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        machineId: String,
        machineName: String,
        assignedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun assignOperator(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        operatorId: String,
        operatorName: String,
        assignedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun holdJob(
        tenantId: String,
        executionJobId: String,
        workOrderId: String?,
        category: HoldCategory,
        reason: String,
        heldBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun releaseHold(
        tenantId: String,
        executionJobId: String,
        resolutionNotes: String?,
        releasedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun recordWastage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        materialCode: String,
        quantity: BigDecimal,
        unitOfMeasure: String,
        reason: String,
        recordedBy: String
    ): DomainResult<ProductionWastageRecord>

    suspend fun createRework(
        tenantId: String,
        executionJobId: String,
        sourceWorkOrderId: String,
        targetWorkOrderId: String,
        quantity: BigDecimal,
        defectCode: String?,
        reason: String,
        requestedBy: String
    ): DomainResult<ProductionReworkRecord>

    suspend fun requestQc(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        requestedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun completeJob(
        tenantId: String,
        executionJobId: String,
        summary: String?,
        completedBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun cancelJob(
        tenantId: String,
        executionJobId: String,
        reason: String,
        cancelledBy: String
    ): DomainResult<ProductionJobExecution>

    suspend fun reconcileJob(
        tenantId: String,
        executionJobId: String
    ): DomainResult<ProductionExecutionReconciliationResult>

    suspend fun exportHandoffContract(
        tenantId: String,
        executionJobId: String
    ): DomainResult<Module17Step05ProductionExecutionHandoffContract>

    suspend fun listExecutionEvents(
        tenantId: String,
        executionJobId: String
    ): DomainResult<List<ProductionExecutionEvent>>
}

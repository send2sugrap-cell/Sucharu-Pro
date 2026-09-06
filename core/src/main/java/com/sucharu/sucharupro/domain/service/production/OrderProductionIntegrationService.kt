package com.sucharu.sucharupro.domain.service.production

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionExecutionDiagnostic
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution

/**
 * Domain Service Interface for Order → Production Integration (Phase 05 Step 01).
 *
 * Provides commercial order validation, production eligibility checks, duplicate job prevention,
 * and traceable Production Job creation from confirmed customer orders.
 */
interface OrderProductionIntegrationService {

    /**
     * Evaluates if a commercial order is eligible for production job creation.
     */
    suspend fun evaluateOrderEligibility(
        tenantId: String,
        orderId: String
    ): DomainResult<List<ProductionExecutionDiagnostic>>

    /**
     * Creates or retrieves an existing Production Job for a confirmed Order.
     * Prevents duplicate job creation idempotently.
     */
    suspend fun createProductionJobFromOrder(
        tenantId: String,
        orderId: String,
        requestedBy: String,
        idempotencyKey: String? = null
    ): DomainResult<ProductionJobExecution>

    /**
     * Retrieves the existing Production Job associated with an Order ID, if present.
     */
    suspend fun getProductionJobForOrder(
        tenantId: String,
        orderId: String
    ): DomainResult<ProductionJobExecution?>
}

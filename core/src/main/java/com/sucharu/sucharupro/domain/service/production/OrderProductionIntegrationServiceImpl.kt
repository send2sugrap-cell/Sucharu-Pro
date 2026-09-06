package com.sucharu.sucharupro.domain.service.production

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionExecutionDiagnostic
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionService

/**
 * Production-grade implementation of [OrderProductionIntegrationService].
 */
class OrderProductionIntegrationServiceImpl(
    private val productionExecutionService: ProductionExecutionService,
    private val orderRepository: OrderRepository
) : OrderProductionIntegrationService {

    override suspend fun evaluateOrderEligibility(
        tenantId: String,
        orderId: String
    ): DomainResult<List<ProductionExecutionDiagnostic>> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required.")
        }
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID is required.")
        }

        val order = when (val res = orderRepository.findOrderById(orderId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = res.message ?: "Order '$orderId' not found.")
            DomainResult.Loading -> return DomainResult.Error(message = "Loading order data.")
        }

        if (order.status == OrderStatusType.CANCELLED) {
            return DomainResult.Success(
                listOf(
                    ProductionExecutionDiagnostic(
                        code = "ORDER_CANCELLED",
                        message = "Order '${order.orderNumber}' is CANCELLED. Cannot create production job.",
                        isBlocking = true,
                        recommendedAction = "Verify order status."
                    )
                )
            )
        }

        return productionExecutionService.evaluateJobEligibility(tenantId, orderId)
    }

    override suspend fun createProductionJobFromOrder(
        tenantId: String,
        orderId: String,
        requestedBy: String,
        idempotencyKey: String?
    ): DomainResult<ProductionJobExecution> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required.")
        }
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID is required.")
        }

        val order = when (val res = orderRepository.findOrderById(orderId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = res.message ?: "Order '$orderId' not found.")
            DomainResult.Loading -> return DomainResult.Error(message = "Loading order data.")
        }

        if (order.status == OrderStatusType.CANCELLED) {
            return DomainResult.Error(message = "Cannot create production job for CANCELLED order '${order.orderNumber}'.")
        }

        // Check if a job already exists for this order (Duplicate Job Prevention)
        val existingJobsRes = productionExecutionService.listJobExecutionsByOrder(tenantId, orderId)
        if (existingJobsRes is DomainResult.Success) {
            val activeJob = existingJobsRes.data.firstOrNull { !it.isCompleted }
            if (activeJob != null) {
                return DomainResult.Success(activeJob)
            }
        }

        return productionExecutionService.createJobExecution(
            tenantId = tenantId,
            orderId = orderId,
            requestedBy = requestedBy,
            idempotencyKey = idempotencyKey ?: "idemp-order-prod-$orderId"
        )
    }

    override suspend fun getProductionJobForOrder(
        tenantId: String,
        orderId: String
    ): DomainResult<ProductionJobExecution?> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required.")
        }
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID is required.")
        }

        val existingJobsRes = productionExecutionService.listJobExecutionsByOrder(tenantId, orderId)
        return when (existingJobsRes) {
            is DomainResult.Success -> DomainResult.Success(existingJobsRes.data.firstOrNull())
            is DomainResult.Error -> DomainResult.Error(message = existingJobsRes.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }
}

package com.sucharu.sucharupro.domain.service.erp

import com.sucharu.sucharupro.data.api.model.erp.OrchestrateOrderActionRequestDto
import com.sucharu.sucharupro.data.api.model.erp.OrchestratedOrderResultDto
import com.sucharu.sucharupro.domain.model.erp.CustomerActionType
import com.sucharu.sucharupro.domain.model.erp.ErpWorkflowOrchestration
import com.sucharu.sucharupro.domain.model.erp.ErpWorkflowStatus
import com.sucharu.sucharupro.domain.repository.ErpWorkflowRepository
import com.sucharu.sucharupro.domain.service.pricing.CommercialPricingService
import java.util.UUID

/**
 * Domain Orchestration Service for Form 05 — ERP / Order / Fulfillment Integration.
 */
class ErpWorkflowOrchestrationService(
    private val erpRepository: ErpWorkflowRepository,
    private val pricingService: CommercialPricingService
) {

    /**
     * Orchestrates Customer Action -> Form 04 Commercial Price Evaluation -> Order -> ERP Pipeline.
     */
    suspend fun orchestrateCustomerOrderAction(request: OrchestrateOrderActionRequestDto): OrchestratedOrderResultDto {
        val actionType = try { CustomerActionType.valueOf(request.customerAction) } catch (e: Exception) { CustomerActionType.ORDER_NOW }
        val orchestrationId = "ORCH-" + UUID.randomUUID().toString().take(8).uppercase()
        val quotationId = "QUOTE-" + UUID.randomUUID().toString().take(8).uppercase()
        val orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()
        val timestamp = "2026-09-26T14:00:00Z"

        // 1. Evaluate Commercial Price via Form 04
        val priceEval = pricingService.evaluateCommercialPrice(
            productId = request.productId,
            quantity = request.orderQuantity,
            customerId = request.customerId,
            offerId = request.offerId,
            deliveryZone = request.deliveryZone
        )

        // 2. Save Immutable Order Price Snapshot via Form 04
        val priceSnapshot = pricingService.createOrderPriceSnapshot(orderId, priceEval)

        // 3. Create ERP Workflow Orchestration Record
        val orchestration = ErpWorkflowOrchestration(
            orchestrationId = orchestrationId,
            customerAction = actionType,
            customerId = request.customerId,
            productId = request.productId,
            offerId = request.offerId,
            priceConfigId = priceEval.priceConfigId,
            workflowStatus = ErpWorkflowStatus.ORDER_CONFIRMED,
            quotationId = quotationId,
            orderId = orderId,
            orderQuantity = request.orderQuantity,
            orderAmount = priceEval.grandTotalAmount,
            currency = priceEval.currency,
            specialInstructions = request.specialInstructions,
            createdAt = timestamp,
            updatedAt = timestamp,
            createdBy = request.customerId
        )

        erpRepository.saveOrchestration(orchestration)

        return OrchestratedOrderResultDto(
            orchestrationId = orchestrationId,
            workflowStatus = ErpWorkflowStatus.ORDER_CONFIRMED.name,
            customerId = request.customerId,
            productId = request.productId,
            quotationId = quotationId,
            orderId = orderId,
            priceSnapshotId = priceSnapshot.snapshotId,
            grandTotalAmount = priceEval.grandTotalAmount,
            currency = priceEval.currency,
            message = "🎉 Order #$orderId successfully confirmed and price snapshot ৳${priceEval.grandTotalAmount.toInt()} saved!"
        )
    }

    suspend fun getOrchestrationByOrder(orderId: String): ErpWorkflowOrchestration? {
        return erpRepository.getOrchestrationByOrder(orderId)
    }

    suspend fun listAllOrchestrations(): List<ErpWorkflowOrchestration> {
        return erpRepository.getAllOrchestrations()
    }
}

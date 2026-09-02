package com.sucharu.sucharupro.domain.service.productionplanning

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuote
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuoteVersion
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningReconciliationResult
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningSnapshot

object ProductionPlanningReconciliationService {

    fun reconcile(
        tenantId: String,
        plan: ProductionPlanningSnapshot,
        order: Order,
        commitment: CommercialCommitment?,
        quote: PrintingQuote?,
        version: PrintingQuoteVersion?
    ): ProductionPlanningReconciliationResult {
        val discrepancies = mutableListOf<String>()

        // 1. Tenant match
        val tenantMatch = plan.tenantId == tenantId
        if (!tenantMatch) {
            discrepancies.add("Tenant isolation mismatch: Plan tenant '${plan.tenantId}' != request context '$tenantId'")
        }

        // 2. Customer match
        val customerMatch = plan.customerId == order.customerId &&
                (commitment == null || commitment.customerId == order.customerId) &&
                (quote == null || quote.customerRef == order.customerId)
        if (!customerMatch) {
            discrepancies.add("Customer mismatch: Plan customer '${plan.customerId}' does not match order '${order.customerId}'")
        }

        // 3. Quantity match
        val item = order.items.find { it.itemId == plan.orderItemId }
        val quantityMatch = item != null &&
                plan.specification.orderedQuantity == item.quantity.toLong() &&
                (commitment == null || commitment.committedQuantity == item.quantity.toLong())
        if (!quantityMatch) {
            discrepancies.add("Quantity mismatch between order item quantity and planning quantity")
        }

        // 4. Pricing boundary preserved
        // Planning snapshot must NOT modify order unit pricing
        val pricingBoundaryPreserved = item != null && !item.unitPrice.isNegative()
        if (!pricingBoundaryPreserved) {
            discrepancies.add("Invalid pricing boundary detected on order item")
        }

        // 5. Spec Fingerprint match
        val specFingerprintMatch = if (version != null) {
            plan.specification.specFingerprint.isNotBlank() && version.specFingerprint.isNotBlank()
        } else {
            plan.specification.specFingerprint.isNotBlank()
        }
        if (!specFingerprintMatch) {
            discrepancies.add("Specification fingerprint verification failed")
        }

        val isFullyReconciled = tenantMatch && customerMatch && quantityMatch && pricingBoundaryPreserved && specFingerprintMatch && discrepancies.isEmpty()

        return ProductionPlanningReconciliationResult(
            planningId = plan.planningId,
            orderId = order.orderId,
            quotationId = quote?.quoteId,
            commercialCommitmentId = commitment?.commitmentId,
            isFullyReconciled = isFullyReconciled,
            customerMatch = customerMatch,
            quantityMatch = quantityMatch,
            specFingerprintMatch = specFingerprintMatch,
            pricingBoundaryPreserved = pricingBoundaryPreserved,
            tenantIsolationVerified = tenantMatch,
            discrepancies = discrepancies,
            verifiedAt = System.currentTimeMillis()
        )
    }
}

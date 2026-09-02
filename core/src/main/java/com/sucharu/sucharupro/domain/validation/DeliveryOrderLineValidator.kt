package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine

/**
 * Domain validator for Delivery Order Lines (Module 08 Step 01).
 */
object DeliveryOrderLineValidator {

    /**
     * Validates structural invariants of a [DeliveryOrderLine].
     */
    fun validateLine(line: DeliveryOrderLine): DomainResult<Unit> {
        if (line.lineId.isBlank()) {
            return DomainResult.Error(message = "Line ID cannot be blank.")
        }
        if (line.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.productId.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }
        if (line.requestedQuantity <= 0) {
            return DomainResult.Error(message = "Requested quantity must be greater than zero.")
        }
        return DomainResult.Success(Unit)
    }
}

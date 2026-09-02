package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine

/**
 * Validates individual DeliveryPartialSettlementLine items (Module 08 Step 06).
 */
object DeliveryPartialSettlementLineValidator {

    fun validateLine(line: DeliveryPartialSettlementLine): DomainResult<Unit> {
        if (line.settlementLineId.isBlank()) {
            return DomainResult.Error(message = "Settlement Line ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.settlementId.isBlank()) {
            return DomainResult.Error(message = "Settlement ID cannot be blank.")
        }
        if (line.deliveryOrderLineId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order Line ID cannot be blank.")
        }
        if (line.productId.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }
        if (line.orderedQuantity < 0) {
            return DomainResult.Error(message = "Ordered quantity cannot be negative.")
        }
        if (line.allocatedQuantity < 0) {
            return DomainResult.Error(message = "Allocated quantity cannot be negative.")
        }
        if (line.dispatchedQuantity < 0) {
            return DomainResult.Error(message = "Dispatched quantity cannot be negative.")
        }
        if (line.deliveredQuantity < 0) {
            return DomainResult.Error(message = "Delivered quantity cannot be negative.")
        }
        if (line.shortQuantity < 0) {
            return DomainResult.Error(message = "Short quantity cannot be negative.")
        }
        if (line.excessQuantity < 0) {
            return DomainResult.Error(message = "Excess quantity cannot be negative.")
        }
        if (line.returnedQuantity < 0) {
            return DomainResult.Error(message = "Returned quantity cannot be negative.")
        }
        if (line.replacementQuantity < 0) {
            return DomainResult.Error(message = "Replacement quantity cannot be negative.")
        }
        if (line.pendingQuantity < 0) {
            return DomainResult.Error(message = "Pending quantity cannot be negative.")
        }
        if (line.createdAt <= 0) {
            return DomainResult.Error(message = "Created timestamp must be positive.")
        }
        if (line.updatedAt < line.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot be before created timestamp.")
        }

        return DomainResult.Success(Unit)
    }
}

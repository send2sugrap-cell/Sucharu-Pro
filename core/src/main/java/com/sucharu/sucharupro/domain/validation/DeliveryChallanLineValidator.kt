package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine

/**
 * Validator for structural integrity of [DeliveryChallanLine] (Module 08 Step 02).
 */
object DeliveryChallanLineValidator {

    fun validateLine(line: DeliveryChallanLine): DomainResult<Unit> {
        if (line.lineId.isBlank()) {
            return DomainResult.Error(message = "Line ID cannot be blank.")
        }
        if (line.challanId.isBlank()) {
            return DomainResult.Error(message = "Challan ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.deliveryOrderLineId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order Line ID cannot be blank.")
        }
        if (line.productId.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }
        if (line.quantity <= 0) {
            return DomainResult.Error(message = "Quantity must be greater than zero.")
        }
        return DomainResult.Success(Unit)
    }
}

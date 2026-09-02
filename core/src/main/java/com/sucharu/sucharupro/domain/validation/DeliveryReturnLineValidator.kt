package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine

/**
 * Validation rules for Delivery Return Lines (Module 08 Step 07).
 */
object DeliveryReturnLineValidator {

    fun validateLine(
        line: DeliveryReturnLine,
        maxEligibleReturnQuantity: Double? = null
    ): DomainResult<Unit> {
        if (line.returnedQuantity <= 0) {
            return DomainResult.Error(message = "Returned quantity must be strictly positive (> 0).")
        }

        if (maxEligibleReturnQuantity != null && line.returnedQuantity > maxEligibleReturnQuantity + 0.001) {
            return DomainResult.Error(
                message = "Requested return quantity (${line.returnedQuantity}) exceeds max eligible returnable quantity ($maxEligibleReturnQuantity) for line '${line.deliveryOrderLineId}'."
            )
        }

        if (line.receivedQuantity < 0) {
            return DomainResult.Error(message = "Received quantity cannot be negative.")
        }

        if (line.acceptedQuantity < 0) {
            return DomainResult.Error(message = "Accepted quantity cannot be negative.")
        }

        if (line.rejectedQuantity < 0) {
            return DomainResult.Error(message = "Rejected quantity cannot be negative.")
        }

        val baseQty = if (line.receivedQuantity > 0) line.receivedQuantity else line.returnedQuantity
        if (line.acceptedQuantity + line.rejectedQuantity > baseQty + 0.001) {
            return DomainResult.Error(
                message = "Sum of accepted (${line.acceptedQuantity}) and rejected (${line.rejectedQuantity}) cannot exceed available quantity ($baseQty)."
            )
        }

        if (line.restockedQuantity < 0) {
            return DomainResult.Error(message = "Restocked quantity cannot be negative.")
        }

        if (line.restockedQuantity > line.acceptedQuantity + 0.001) {
            return DomainResult.Error(
                message = "Restocked quantity (${line.restockedQuantity}) cannot exceed accepted quantity (${line.acceptedQuantity})."
            )
        }

        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine

/**
 * Structural and domain validator for [DispatchExecutionLine] (Module 08 Step 03).
 */
object DispatchExecutionLineValidator {

    fun validateLine(line: DispatchExecutionLine): DomainResult<Unit> {
        if (line.dispatchExecutionLineId.isBlank()) return DomainResult.Error(message = "Dispatch execution line ID cannot be blank.")
        if (line.projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (line.dispatchExecutionId.isBlank()) return DomainResult.Error(message = "Dispatch execution ID cannot be blank.")
        if (line.deliveryChallanLineId.isBlank()) return DomainResult.Error(message = "Delivery challan line ID cannot be blank.")
        if (line.deliveryOrderLineId.isBlank()) return DomainResult.Error(message = "Delivery order line ID cannot be blank.")
        if (line.productId.isBlank()) return DomainResult.Error(message = "Product ID cannot be blank.")
        if (line.sourceLocationId.isBlank()) return DomainResult.Error(message = "Source location ID cannot be blank.")
        if (line.requestedQuantity <= 0) return DomainResult.Error(message = "Requested quantity must be positive.")
        if (line.dispatchQuantity <= 0) return DomainResult.Error(message = "Dispatch quantity must be positive.")
        if (line.dispatchQuantity > line.requestedQuantity) {
            return DomainResult.Error(
                message = "Dispatch quantity (${line.dispatchQuantity}) cannot exceed requested quantity (${line.requestedQuantity})."
            )
        }
        if (line.createdAt <= 0) return DomainResult.Error(message = "Created at timestamp must be positive.")
        return DomainResult.Success(Unit)
    }
}

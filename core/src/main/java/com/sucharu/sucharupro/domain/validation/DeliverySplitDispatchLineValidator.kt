package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine

/**
 * Validates individual DeliverySplitDispatchLine items (Module 08 Step 06).
 */
object DeliverySplitDispatchLineValidator {

    fun validateLine(
        line: DeliverySplitDispatchLine,
        remainingAuthorizedQuantity: Double? = null
    ): DomainResult<Unit> {
        if (line.splitDispatchLineId.isBlank()) {
            return DomainResult.Error(message = "Split Dispatch Line ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.splitDispatchId.isBlank()) {
            return DomainResult.Error(message = "Split Dispatch ID cannot be blank.")
        }
        if (line.deliveryOrderLineId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order Line ID cannot be blank.")
        }
        if (line.productId.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }
        if (line.quantity <= 0) {
            return DomainResult.Error(message = "Split quantity must be strictly positive (> 0).")
        }
        if (line.createdAt <= 0) {
            return DomainResult.Error(message = "Created timestamp must be positive.")
        }

        if (remainingAuthorizedQuantity != null && line.quantity > remainingAuthorizedQuantity) {
            return DomainResult.Error(
                message = "Split quantity (${line.quantity}) exceeds remaining authorized quantity ($remainingAuthorizedQuantity) for item '${line.productId}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}

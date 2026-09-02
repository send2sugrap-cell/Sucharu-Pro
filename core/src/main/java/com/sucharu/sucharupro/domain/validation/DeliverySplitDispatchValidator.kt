package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine

/**
 * Validates logical split dispatches and quantity allocation against settlement state (Module 08 Step 06).
 */
object DeliverySplitDispatchValidator {

    fun validateSplitDispatch(
        split: DeliverySplitDispatch,
        lines: List<DeliverySplitDispatchLine>,
        existingSplits: List<DeliverySplitDispatch>,
        settlementLines: List<DeliveryPartialSettlementLine>? = null
    ): DomainResult<Unit> {
        if (split.splitDispatchId.isBlank()) {
            return DomainResult.Error(message = "Split Dispatch ID cannot be blank.")
        }
        if (split.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (split.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        }
        if (split.splitSequence <= 0) {
            return DomainResult.Error(message = "Split sequence must be positive (>= 1).")
        }
        if (split.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created by cannot be blank.")
        }
        if (split.createdAt <= 0) {
            return DomainResult.Error(message = "Created timestamp must be positive.")
        }
        if (split.updatedAt < split.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot be before created timestamp.")
        }
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Split dispatch must contain at least one line item.")
        }

        // Sequence uniqueness check
        if (existingSplits.any { it.splitDispatchId != split.splitDispatchId && it.splitSequence == split.splitSequence }) {
            return DomainResult.Error(
                message = "Split sequence #${split.splitSequence} already exists for Delivery Order '${split.deliveryOrderId}'."
            )
        }

        // Line-level validation
        for (line in lines) {
            val sLine = settlementLines?.find { it.deliveryOrderLineId == line.deliveryOrderLineId }
            val lineRes = DeliverySplitDispatchLineValidator.validateLine(line, sLine?.pendingQuantity)
            if (lineRes is DomainResult.Error) return lineRes

            if (line.projectId != split.projectId) {
                return DomainResult.Error(
                    message = "Project mismatch: Line belongs to '${line.projectId}', but split is in '${split.projectId}'."
                )
            }
            if (line.splitDispatchId != split.splitDispatchId) {
                return DomainResult.Error(
                    message = "Split ID mismatch: Line references '${line.splitDispatchId}', but parent split is '${split.splitDispatchId}'."
                )
            }
        }

        return DomainResult.Success(Unit)
    }
}

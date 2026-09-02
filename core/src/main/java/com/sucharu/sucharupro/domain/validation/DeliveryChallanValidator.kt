package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine

/**
 * Domain validator for Delivery Challans (Module 08 Step 02).
 */
object DeliveryChallanValidator {

    /**
     * Validates structural invariants and cross-entity consistency for a [DeliveryChallan].
     */
    fun validateChallan(challan: DeliveryChallan, lines: List<DeliveryChallanLine>): DomainResult<Unit> {
        if (challan.challanId.isBlank()) return DomainResult.Error(message = "Challan ID cannot be blank.")
        if (challan.projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (challan.challanNo.isBlank()) return DomainResult.Error(message = "Challan Number cannot be blank.")
        if (challan.deliveryOrderId.isBlank()) return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        if (challan.createdBy.isBlank()) return DomainResult.Error(message = "Created By cannot be blank.")
        if (challan.issueDate <= 0) return DomainResult.Error(message = "Issue Date must be positive.")
        if (challan.createdAt <= 0) return DomainResult.Error(message = "Created At timestamp must be positive.")
        if (challan.updatedAt < challan.createdAt) {
            return DomainResult.Error(message = "Updated At cannot be before Created At.")
        }

        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Delivery Challan must have at least one line item.")
        }

        for (line in lines) {
            if (line.projectId != challan.projectId) {
                return DomainResult.Error(
                    message = "Project ID mismatch: Line '${line.lineId}' belongs to project '${line.projectId}', but challan belongs to '${challan.projectId}'."
                )
            }
            if (line.challanId != challan.challanId) {
                return DomainResult.Error(
                    message = "Challan ID mismatch: Line '${line.lineId}' references challan '${line.challanId}', but current challan is '${challan.challanId}'."
                )
            }

            val lineResult = DeliveryChallanLineValidator.validateLine(line)
            if (lineResult is DomainResult.Error) return lineResult
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the referenced Delivery Order is eligible for Challan generation.
     */
    fun validateDeliveryOrderEligibility(order: DeliveryOrder, targetProjectId: String): DomainResult<Unit> {
        if (order.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Delivery Order belongs to '${order.projectId}', but challan belongs to '$targetProjectId'."
            )
        }

        return when (order.status) {
            DeliveryOrderStatus.APPROVED,
            DeliveryOrderStatus.READY_FOR_DISPATCH -> DomainResult.Success(Unit)
            DeliveryOrderStatus.DRAFT,
            DeliveryOrderStatus.PENDING -> DomainResult.Error(
                message = "Delivery Order '${order.deliveryOrderNo}' is in '${order.status}' status. Only APPROVED or READY_FOR_DISPATCH orders can have Challans created."
            )
            DeliveryOrderStatus.CANCELLED -> DomainResult.Error(
                message = "Cannot create Challan for CANCELLED Delivery Order '${order.deliveryOrderNo}'."
            )
            else -> DomainResult.Error(
                message = "Delivery Order '${order.deliveryOrderNo}' status '${order.status}' is not eligible for Challan generation."
            )
        }
    }

    /**
     * Validates that Challan lines do not exceed the available allocatable quantities on the Delivery Order lines.
     */
    fun validateQuantityAllocation(
        orderLines: List<DeliveryOrderLine>,
        existingActiveChallanLines: List<DeliveryChallanLine>,
        newChallanLines: List<DeliveryChallanLine>
    ): DomainResult<Unit> {
        val orderLineMap = orderLines.associateBy { it.lineId }

        // Sum existing active allocations by deliveryOrderLineId
        val existingAllocationMap = existingActiveChallanLines
            .groupBy { it.deliveryOrderLineId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }

        // Sum new challan quantities by deliveryOrderLineId
        val newAllocationMap = newChallanLines
            .groupBy { it.deliveryOrderLineId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }

        for ((doLineId, newQty) in newAllocationMap) {
            val doLine = orderLineMap[doLineId]
                ?: return DomainResult.Error(message = "Referenced Delivery Order line '$doLineId' does not exist.")

            // Verify product match
            val mismatchedProductLine = newChallanLines.find { it.deliveryOrderLineId == doLineId && it.productId != doLine.productId }
            if (mismatchedProductLine != null) {
                return DomainResult.Error(
                    message = "Product mismatch on line '${mismatchedProductLine.lineId}': Challan specifies product '${mismatchedProductLine.productId}', but Delivery Order line requires '${doLine.productId}'."
                )
            }

            val existingAllocated = existingAllocationMap[doLineId] ?: 0.0
            val totalAllocated = existingAllocated + newQty

            if (totalAllocated > doLine.requestedQuantity) {
                val remaining = (doLine.requestedQuantity - existingAllocated).coerceAtLeast(0.0)
                return DomainResult.Error(
                    message = "Over-allocation on product '${doLine.productId}': Requested $newQty, but only $remaining remaining out of ${doLine.requestedQuantity} (already allocated $existingAllocated)."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that immutable identity fields have not been altered during an update.
     */
    fun validateImmutableIdentity(original: DeliveryChallan, updated: DeliveryChallan): DomainResult<Unit> {
        if (original.challanId != updated.challanId) {
            return DomainResult.Error(message = "Challan ID is immutable and cannot be changed.")
        }
        if (original.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable and cannot be changed.")
        }
        if (original.challanNo != updated.challanNo) {
            return DomainResult.Error(message = "Challan Number is immutable and cannot be changed.")
        }
        if (original.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order ID reference is immutable and cannot be changed.")
        }
        if (original.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Created By is immutable and cannot be changed.")
        }
        if (original.createdAt != updated.createdAt) {
            return DomainResult.Error(message = "Created At timestamp is immutable and cannot be changed.")
        }
        return DomainResult.Success(Unit)
    }
}

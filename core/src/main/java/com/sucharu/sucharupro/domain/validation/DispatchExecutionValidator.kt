package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine

/**
 * Domain validator for Dispatch Executions (Module 08 Step 03).
 */
object DispatchExecutionValidator {

    /**
     * Validates structural invariants and line associations.
     */
    fun validateDispatchExecution(
        dispatch: DispatchExecution,
        lines: List<DispatchExecutionLine>
    ): DomainResult<Unit> {
        if (dispatch.dispatchExecutionId.isBlank()) return DomainResult.Error(message = "Dispatch execution ID cannot be blank.")
        if (dispatch.projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (dispatch.dispatchNo.isBlank()) return DomainResult.Error(message = "Dispatch number cannot be blank.")
        if (dispatch.deliveryOrderId.isBlank()) return DomainResult.Error(message = "Delivery order ID cannot be blank.")
        if (dispatch.deliveryChallanId.isBlank()) return DomainResult.Error(message = "Delivery challan ID cannot be blank.")
        if (dispatch.sourceWarehouseId.isBlank()) return DomainResult.Error(message = "Source warehouse ID cannot be blank.")
        if (dispatch.sourceLocationId.isBlank()) return DomainResult.Error(message = "Source location ID cannot be blank.")
        if (dispatch.createdBy.isBlank()) return DomainResult.Error(message = "Created by actor cannot be blank.")
        if (dispatch.dispatchDate <= 0) return DomainResult.Error(message = "Dispatch date must be positive.")
        if (dispatch.createdAt <= 0) return DomainResult.Error(message = "Created at timestamp must be positive.")
        if (dispatch.updatedAt < dispatch.createdAt) {
            return DomainResult.Error(message = "Updated at timestamp cannot precede Created at.")
        }

        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Dispatch execution must contain at least one line item.")
        }

        for (line in lines) {
            if (line.projectId != dispatch.projectId) {
                return DomainResult.Error(
                    message = "Project mismatch: Line '${line.dispatchExecutionLineId}' belongs to project '${line.projectId}', but dispatch belongs to '${dispatch.projectId}'."
                )
            }
            if (line.dispatchExecutionId != dispatch.dispatchExecutionId) {
                return DomainResult.Error(
                    message = "Dispatch ID mismatch: Line '${line.dispatchExecutionLineId}' references dispatch '${line.dispatchExecutionId}', but current dispatch is '${dispatch.dispatchExecutionId}'."
                )
            }
            val lineResult = DispatchExecutionLineValidator.validateLine(line)
            if (lineResult is DomainResult.Error) return lineResult
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the referenced Delivery Challan is in an eligible status for dispatch execution.
     */
    fun validateChallanEligibility(
        challan: DeliveryChallan,
        targetProjectId: String
    ): DomainResult<Unit> {
        if (challan.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Delivery Challan belongs to '${challan.projectId}', but dispatch belongs to '$targetProjectId'."
            )
        }

        return when (challan.status) {
            DeliveryChallanStatus.APPROVED,
            DeliveryChallanStatus.READY_FOR_DISPATCH -> DomainResult.Success(Unit)
            DeliveryChallanStatus.DRAFT,
            DeliveryChallanStatus.PENDING -> DomainResult.Error(
                message = "Delivery Challan '${challan.challanNo}' is in '${challan.status}' status. Only APPROVED or READY_FOR_DISPATCH Challans can be dispatched."
            )
            DeliveryChallanStatus.DISPATCHED -> DomainResult.Error(
                message = "Delivery Challan '${challan.challanNo}' is already DISPATCHED."
            )
            DeliveryChallanStatus.DELIVERED -> DomainResult.Error(
                message = "Delivery Challan '${challan.challanNo}' is already DELIVERED."
            )
            DeliveryChallanStatus.CANCELLED -> DomainResult.Error(
                message = "Cannot dispatch CANCELLED Delivery Challan '${challan.challanNo}'."
            )
        }
    }

    /**
     * Validates that dispatch lines match the authorized Challan lines and quantities.
     */
    fun validateLinesAgainstChallan(
        challanLines: List<DeliveryChallanLine>,
        dispatchLines: List<DispatchExecutionLine>
    ): DomainResult<Unit> {
        val challanLineMap = challanLines.associateBy { it.lineId }

        for (dispatchLine in dispatchLines) {
            val challanLine = challanLineMap[dispatchLine.deliveryChallanLineId]
                ?: return DomainResult.Error(
                    message = "Referenced Challan line '${dispatchLine.deliveryChallanLineId}' does not exist on this Delivery Challan."
                )

            if (dispatchLine.productId != challanLine.productId) {
                return DomainResult.Error(
                    message = "Product mismatch on line '${dispatchLine.dispatchExecutionLineId}': Dispatch specifies '${dispatchLine.productId}', but Challan line requires '${challanLine.productId}'."
                )
            }

            if (dispatchLine.dispatchQuantity > challanLine.quantity) {
                return DomainResult.Error(
                    message = "Dispatch quantity (${dispatchLine.dispatchQuantity}) exceeds authorized Challan quantity (${challanLine.quantity}) on product '${dispatchLine.productId}'."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that immutable identity fields cannot be altered.
     */
    fun validateImmutableIdentity(original: DispatchExecution, updated: DispatchExecution): DomainResult<Unit> {
        if (original.dispatchExecutionId != updated.dispatchExecutionId) {
            return DomainResult.Error(message = "Dispatch Execution ID is immutable and cannot be changed.")
        }
        if (original.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable and cannot be changed.")
        }
        if (original.dispatchNo != updated.dispatchNo) {
            return DomainResult.Error(message = "Dispatch Number is immutable and cannot be changed.")
        }
        if (original.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order ID is immutable and cannot be changed.")
        }
        if (original.deliveryChallanId != updated.deliveryChallanId) {
            return DomainResult.Error(message = "Delivery Challan ID is immutable and cannot be changed.")
        }
        if (original.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Created By actor is immutable and cannot be changed.")
        }
        if (original.createdAt != updated.createdAt) {
            return DomainResult.Error(message = "Created At timestamp is immutable and cannot be changed.")
        }
        return DomainResult.Success(Unit)
    }
}

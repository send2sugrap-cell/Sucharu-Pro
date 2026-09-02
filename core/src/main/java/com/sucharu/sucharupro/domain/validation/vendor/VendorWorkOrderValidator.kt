package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderStatus
import java.math.BigDecimal

/**
 * Domain validator enforcing business invariants and lifecycle state transitions on VendorWorkOrder entities (Module 12 Step 04).
 */
object VendorWorkOrderValidator {

    private const val MAX_NUMBER_LENGTH = 64
    private const val MAX_TITLE_LENGTH = 200
    private const val MAX_DESC_LENGTH = 3000
    private const val MAX_NOTES_LENGTH = 3000

    fun validate(workOrder: VendorWorkOrder): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (workOrder.workOrderId.isBlank()) {
            errors.add("workOrderId is required and cannot be blank")
        }
        if (workOrder.projectId.isBlank()) {
            errors.add("projectId is required and cannot be blank")
        }
        if (workOrder.workOrderNumber.isBlank()) {
            errors.add("workOrderNumber is required and cannot be blank")
        } else if (workOrder.workOrderNumber.length > MAX_NUMBER_LENGTH) {
            errors.add("workOrderNumber exceeds maximum length of $MAX_NUMBER_LENGTH characters")
        }
        if (workOrder.vendorId.isBlank()) {
            errors.add("vendorId is required and cannot be blank")
        }
        if (workOrder.title.isBlank()) {
            errors.add("title is required and cannot be blank")
        } else if (workOrder.title.length > MAX_TITLE_LENGTH) {
            errors.add("title exceeds maximum length of $MAX_TITLE_LENGTH characters")
        }

        if (workOrder.quantity <= BigDecimal.ZERO) {
            errors.add("quantity must be strictly greater than zero")
        }

        if (workOrder.estimatedAmount.isNegative()) {
            errors.add("estimatedAmount cannot be negative")
        }

        if (workOrder.scheduledStartAt != null && workOrder.scheduledDueAt != null) {
            if (workOrder.scheduledDueAt < workOrder.scheduledStartAt) {
                errors.add("scheduledDueAt cannot be earlier than scheduledStartAt")
            }
        }

        workOrder.description?.let {
            if (it.length > MAX_DESC_LENGTH) {
                errors.add("description exceeds maximum length of $MAX_DESC_LENGTH characters")
            }
        }

        workOrder.notes?.let {
            if (it.length > MAX_NOTES_LENGTH) {
                errors.add("notes exceed maximum length of $MAX_NOTES_LENGTH characters")
            }
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }

    fun validateStatusTransition(current: VendorWorkOrderStatus, target: VendorWorkOrderStatus): VendorValidationResult {
        return if (current.canTransitionTo(target)) {
            VendorValidationResult(true)
        } else {
            VendorValidationResult(
                false,
                listOf("Illegal work order status transition from '${current.name}' to '${target.name}'.")
            )
        }
    }
}

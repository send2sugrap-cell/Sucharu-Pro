package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import java.math.BigDecimal

/**
 * Domain validator enforcing business invariants, line item integrity, and state transitions for VendorPurchaseOrder (Module 12 Step 05).
 */
object VendorPurchaseOrderValidator {

    private const val MAX_NUMBER_LENGTH = 64
    private const val MAX_LOCATION_LENGTH = 300
    private const val MAX_NOTES_LENGTH = 3000
    private const val MAX_DESC_LENGTH = 1000

    fun validate(order: VendorPurchaseOrder): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (order.purchaseOrderId.isBlank()) {
            errors.add("purchaseOrderId is required and cannot be blank")
        }
        if (order.projectId.isBlank()) {
            errors.add("projectId is required and cannot be blank")
        }
        if (order.orderNumber.isBlank()) {
            errors.add("orderNumber is required and cannot be blank")
        } else if (order.orderNumber.length > MAX_NUMBER_LENGTH) {
            errors.add("orderNumber exceeds maximum length of $MAX_NUMBER_LENGTH characters")
        }
        if (order.vendorId.isBlank()) {
            errors.add("vendorId is required and cannot be blank")
        }
        if (order.requestedBy.isBlank()) {
            errors.add("requestedBy is required and cannot be blank")
        }

        if (order.items.isEmpty()) {
            errors.add("Purchase order must contain at least one line item")
        }

        order.items.forEachIndexed { index, item ->
            if (item.itemId.isBlank()) {
                errors.add("Item at index $index has blank itemId")
            }
            if (item.itemDescription.isBlank()) {
                errors.add("Item at index $index has blank itemDescription")
            } else if (item.itemDescription.length > MAX_DESC_LENGTH) {
                errors.add("Item at index $index description exceeds $MAX_DESC_LENGTH characters")
            }
            if (item.quantity <= BigDecimal.ZERO) {
                errors.add("Item '${item.itemDescription}' quantity must be strictly greater than zero")
            }
            if (item.unitRate.isNegative()) {
                errors.add("Item '${item.itemDescription}' unitRate cannot be negative")
            }
            if (item.discount.isNegative()) {
                errors.add("Item '${item.itemDescription}' discount cannot be negative")
            }
            if (item.taxAmount.isNegative()) {
                errors.add("Item '${item.itemDescription}' taxAmount cannot be negative")
            }
            if (item.lineTotal.isNegative()) {
                errors.add("Item '${item.itemDescription}' lineTotal cannot be negative")
            }
        }

        if (order.subtotal.isNegative()) {
            errors.add("subtotal cannot be negative")
        }
        if (order.taxAmount.isNegative()) {
            errors.add("taxAmount cannot be negative")
        }
        if (order.discountAmount.isNegative()) {
            errors.add("discountAmount cannot be negative")
        }
        if (order.totalAmount.isNegative()) {
            errors.add("totalAmount cannot be negative")
        }

        if (order.expectedDeliveryDate != null && order.expectedDeliveryDate < order.orderDate) {
            errors.add("expectedDeliveryDate cannot be earlier than orderDate")
        }

        order.deliveryLocation?.let {
            if (it.length > MAX_LOCATION_LENGTH) {
                errors.add("deliveryLocation exceeds maximum length of $MAX_LOCATION_LENGTH characters")
            }
        }

        order.notes?.let {
            if (it.length > MAX_NOTES_LENGTH) {
                errors.add("notes exceed maximum length of $MAX_NOTES_LENGTH characters")
            }
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }

    fun validateStatusTransition(current: VendorPurchaseOrderStatus, target: VendorPurchaseOrderStatus): VendorValidationResult {
        return if (current.canTransitionTo(target)) {
            VendorValidationResult(true)
        } else {
            VendorValidationResult(
                false,
                listOf("Illegal purchase order status transition from '${current.name}' to '${target.name}'.")
            )
        }
    }

    fun validateApproval(order: VendorPurchaseOrder, approverId: String, allowSelfApproval: Boolean = false): VendorValidationResult {
        if (approverId.isBlank()) {
            return VendorValidationResult(false, listOf("approverId cannot be blank."))
        }
        if (!allowSelfApproval && order.requestedBy == approverId) {
            return VendorValidationResult(
                false,
                listOf("Separation of duties violation: requester '$approverId' cannot approve their own purchase order.")
            )
        }
        return VendorValidationResult(true)
    }
}

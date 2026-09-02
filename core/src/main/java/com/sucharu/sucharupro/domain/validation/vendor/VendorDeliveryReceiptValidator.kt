package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptStatus
import java.math.BigDecimal

/**
 * Domain validator enforcing business invariants, quantity equations, and state transitions for VendorDeliveryReceipt (Module 12 Step 06).
 */
object VendorDeliveryReceiptValidator {

    private const val MAX_NUMBER_LENGTH = 64
    private const val MAX_REF_LENGTH = 100
    private const val MAX_REMARKS_LENGTH = 3000
    private const val MAX_DESC_LENGTH = 1000

    fun validate(receipt: VendorDeliveryReceipt): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (receipt.deliveryReceiptId.isBlank()) {
            errors.add("deliveryReceiptId is required and cannot be blank")
        }
        if (receipt.projectId.isBlank()) {
            errors.add("projectId is required and cannot be blank")
        }
        if (receipt.receiptNumber.isBlank()) {
            errors.add("receiptNumber is required and cannot be blank")
        } else if (receipt.receiptNumber.length > MAX_NUMBER_LENGTH) {
            errors.add("receiptNumber exceeds maximum length of $MAX_NUMBER_LENGTH characters")
        }
        if (receipt.purchaseOrderId.isBlank()) {
            errors.add("purchaseOrderId is required and cannot be blank")
        }
        if (receipt.vendorId.isBlank()) {
            errors.add("vendorId is required and cannot be blank")
        }
        if (receipt.receivedBy.isBlank()) {
            errors.add("receivedBy is required and cannot be blank")
        }

        if (receipt.items.isEmpty()) {
            errors.add("Delivery receipt must contain at least one line item")
        }

        receipt.items.forEachIndexed { index, item ->
            if (item.receiptItemId.isBlank()) {
                errors.add("Item at index $index has blank receiptItemId")
            }
            if (item.purchaseOrderItemId.isBlank()) {
                errors.add("Item at index $index has blank purchaseOrderItemId")
            }
            if (item.itemDescription.isBlank()) {
                errors.add("Item at index $index has blank itemDescription")
            } else if (item.itemDescription.length > MAX_DESC_LENGTH) {
                errors.add("Item at index $index description exceeds $MAX_DESC_LENGTH characters")
            }
            if (item.receivedQuantity < BigDecimal.ZERO) {
                errors.add("Item '${item.itemDescription}' receivedQuantity cannot be negative")
            }
            if (item.acceptedQuantity < BigDecimal.ZERO) {
                errors.add("Item '${item.itemDescription}' acceptedQuantity cannot be negative")
            }
            if (item.rejectedQuantity < BigDecimal.ZERO) {
                errors.add("Item '${item.itemDescription}' rejectedQuantity cannot be negative")
            }
            if (item.damagedQuantity < BigDecimal.ZERO) {
                errors.add("Item '${item.itemDescription}' damagedQuantity cannot be negative")
            }
            if (item.shortQuantity < BigDecimal.ZERO) {
                errors.add("Item '${item.itemDescription}' shortQuantity cannot be negative")
            }
            if (item.excessQuantity < BigDecimal.ZERO) {
                errors.add("Item '${item.itemDescription}' excessQuantity cannot be negative")
            }

            // Quantity equation validation when inspected/accepted
            if (receipt.status.isAccepted || receipt.status.isInspected || receipt.status == VendorDeliveryReceiptStatus.REJECTED) {
                val accounted = item.acceptedQuantity + item.rejectedQuantity + item.damagedQuantity
                if (accounted > item.receivedQuantity + item.excessQuantity) {
                    errors.add("Item '${item.itemDescription}' accounted quantity ($accounted) exceeds received + excess quantity (${item.receivedQuantity + item.excessQuantity})")
                }
            }
        }

        receipt.vendorDeliveryReference?.let {
            if (it.length > MAX_REF_LENGTH) {
                errors.add("vendorDeliveryReference exceeds maximum length of $MAX_REF_LENGTH characters")
            }
        }

        receipt.remarks?.let {
            if (it.length > MAX_REMARKS_LENGTH) {
                errors.add("remarks exceed maximum length of $MAX_REMARKS_LENGTH characters")
            }
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }

    fun validateStatusTransition(current: VendorDeliveryReceiptStatus, target: VendorDeliveryReceiptStatus): VendorValidationResult {
        return if (current.canTransitionTo(target)) {
            VendorValidationResult(true)
        } else {
            VendorValidationResult(
                false,
                listOf("Illegal vendor delivery receipt status transition from '${current.name}' to '${target.name}'.")
            )
        }
    }
}

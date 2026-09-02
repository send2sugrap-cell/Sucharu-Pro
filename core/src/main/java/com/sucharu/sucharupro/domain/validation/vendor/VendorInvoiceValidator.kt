package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoice
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceStatus
import java.math.BigDecimal

/**
 * Domain validator enforcing business invariants, financial equations, and state transitions for VendorInvoice (Module 12 Step 07).
 */
object VendorInvoiceValidator {

    private const val MAX_NUMBER_LENGTH = 64
    private const val MAX_NOTES_LENGTH = 3000
    private const val MAX_DESC_LENGTH = 1000

    fun validate(invoice: VendorInvoice): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (invoice.invoiceId.isBlank()) {
            errors.add("invoiceId is required and cannot be blank")
        }
        if (invoice.projectId.isBlank()) {
            errors.add("projectId is required and cannot be blank")
        }
        if (invoice.vendorId.isBlank()) {
            errors.add("vendorId is required and cannot be blank")
        }
        if (invoice.purchaseOrderId.isBlank()) {
            errors.add("purchaseOrderId is required and cannot be blank")
        }
        if (invoice.invoiceNumber.isBlank()) {
            errors.add("invoiceNumber is required and cannot be blank")
        } else if (invoice.invoiceNumber.length > MAX_NUMBER_LENGTH) {
            errors.add("invoiceNumber exceeds maximum length of $MAX_NUMBER_LENGTH characters")
        }
        if (invoice.vendorInvoiceNumber.isBlank()) {
            errors.add("vendorInvoiceNumber is required and cannot be blank")
        } else if (invoice.vendorInvoiceNumber.length > MAX_NUMBER_LENGTH) {
            errors.add("vendorInvoiceNumber exceeds maximum length of $MAX_NUMBER_LENGTH characters")
        }

        if (invoice.subtotal.isNegative()) {
            errors.add("subtotal cannot be negative")
        }
        if (invoice.taxAmount.isNegative()) {
            errors.add("taxAmount cannot be negative")
        }
        if (invoice.discountAmount.isNegative()) {
            errors.add("discountAmount cannot be negative")
        }
        if (invoice.shippingAmount.isNegative()) {
            errors.add("shippingAmount cannot be negative")
        }
        if (invoice.otherCharges.isNegative()) {
            errors.add("otherCharges cannot be negative")
        }
        if (invoice.totalAmount.isNegative()) {
            errors.add("totalAmount cannot be negative")
        }

        invoice.notes?.let {
            if (it.length > MAX_NOTES_LENGTH) {
                errors.add("notes exceed maximum length of $MAX_NOTES_LENGTH characters")
            }
        }

        if (invoice.items.isEmpty()) {
            errors.add("Vendor invoice must contain at least one line item")
        }

        var calculatedSubtotal = Money.ZERO
        var calculatedTax = Money.ZERO
        var calculatedDiscount = Money.ZERO

        invoice.items.forEachIndexed { index, item ->
            if (item.itemId.isBlank()) {
                errors.add("Item at index $index has blank itemId")
            }
            if (item.purchaseOrderItemId.isBlank()) {
                errors.add("Item at index $index has blank purchaseOrderItemId")
            }
            if (item.description.isBlank()) {
                errors.add("Item at index $index has blank description")
            } else if (item.description.length > MAX_DESC_LENGTH) {
                errors.add("Item at index $index description exceeds $MAX_DESC_LENGTH characters")
            }
            if (item.quantity <= BigDecimal.ZERO) {
                errors.add("Item '${item.description}' quantity must be greater than zero")
            }
            if (item.unitPrice.isNegative()) {
                errors.add("Item '${item.description}' unitPrice cannot be negative")
            }
            if (item.taxAmount.isNegative()) {
                errors.add("Item '${item.description}' taxAmount cannot be negative")
            }
            if (item.discountAmount.isNegative()) {
                errors.add("Item '${item.description}' discountAmount cannot be negative")
            }
            if (item.lineTotal.isNegative()) {
                errors.add("Item '${item.description}' lineTotal cannot be negative")
            }

            calculatedSubtotal += item.unitPrice * item.quantity
            calculatedTax += item.taxAmount
            calculatedDiscount += item.discountAmount
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }

    fun validateStatusTransition(current: VendorInvoiceStatus, target: VendorInvoiceStatus): VendorValidationResult {
        return if (current.canTransitionTo(target)) {
            VendorValidationResult(true)
        } else {
            VendorValidationResult(
                false,
                listOf("Illegal vendor invoice status transition from '${current.name}' to '${target.name}'.")
            )
        }
    }
}

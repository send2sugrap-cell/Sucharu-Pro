package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus

/**
 * Structural and domain validation rules for DeliveryReconciliation aggregate (Module 08 Step 09).
 */
object DeliveryReconciliationValidator {

    fun validateReconciliation(
        reconciliation: DeliveryReconciliation,
        items: List<DeliveryReconciliationItem>,
        targetProjectId: String
    ): DomainResult<Unit> {
        if (reconciliation.reconciliationId.isBlank()) {
            return DomainResult.Error(message = "Reconciliation ID cannot be blank.")
        }
        if (reconciliation.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (reconciliation.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Reconciliation belongs to '${reconciliation.projectId}', but target project is '$targetProjectId'."
            )
        }
        if (reconciliation.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        }
        if (reconciliation.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created by cannot be blank.")
        }
        if (reconciliation.createdAt <= 0) {
            return DomainResult.Error(message = "Creation timestamp must be positive.")
        }
        if (reconciliation.updatedAt < reconciliation.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")
        }

        if (reconciliation.orderedQuantity < 0) {
            return DomainResult.Error(message = "Ordered quantity cannot be negative.")
        }
        if (reconciliation.challanedQuantity < 0) {
            return DomainResult.Error(message = "Challaned quantity cannot be negative.")
        }
        if (reconciliation.dispatchedQuantity < 0) {
            return DomainResult.Error(message = "Dispatched quantity cannot be negative.")
        }
        if (reconciliation.deliveredQuantity < 0) {
            return DomainResult.Error(message = "Delivered quantity cannot be negative.")
        }
        if (reconciliation.acceptedPodQuantity < 0) {
            return DomainResult.Error(message = "Accepted POD quantity cannot be negative.")
        }
        if (reconciliation.rejectedQuantity < 0) {
            return DomainResult.Error(message = "Rejected quantity cannot be negative.")
        }
        if (reconciliation.returnedQuantity < 0) {
            return DomainResult.Error(message = "Returned quantity cannot be negative.")
        }
        if (reconciliation.outstandingQuantity < 0) {
            return DomainResult.Error(message = "Outstanding quantity cannot be negative.")
        }
        if (reconciliation.discrepancyQuantity < 0) {
            return DomainResult.Error(message = "Discrepancy quantity cannot be negative.")
        }

        if (items.isEmpty() && reconciliation.orderedQuantity > 0.0) {
            return DomainResult.Error(message = "Reconciliation must contain at least one item line.")
        }

        for (item in items) {
            if (item.projectId != targetProjectId) {
                return DomainResult.Error(
                    message = "Project isolation violation: Item '${item.reconciliationItemId}' belongs to '${item.projectId}', expected '$targetProjectId'."
                )
            }
            if (item.reconciliationId != reconciliation.reconciliationId) {
                return DomainResult.Error(
                    message = "Reconciliation mismatch: Item '${item.reconciliationItemId}' references '${item.reconciliationId}', expected '${reconciliation.reconciliationId}'."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateImmutableIdentity(
        existing: DeliveryReconciliation,
        updated: DeliveryReconciliation
    ): DomainResult<Unit> {
        if (existing.reconciliationId != updated.reconciliationId) {
            return DomainResult.Error(message = "Reconciliation ID cannot be mutated.")
        }
        if (existing.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID cannot be mutated.")
        }
        if (existing.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order ID cannot be mutated.")
        }
        if (existing.reconciliationStatus == DeliveryReconciliationStatus.CLOSED && updated.reconciliationStatus != DeliveryReconciliationStatus.CLOSED) {
            return DomainResult.Error(message = "Closed reconciliation is immutable and cannot be re-opened.")
        }

        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine

/**
 * Structural and domain integrity validator for DeliveryPartialSettlement (Module 08 Step 06).
 */
object DeliveryPartialSettlementValidator {

    fun validateSettlement(
        settlement: DeliveryPartialSettlement,
        lines: List<DeliveryPartialSettlementLine>
    ): DomainResult<Unit> {
        if (settlement.settlementId.isBlank()) {
            return DomainResult.Error(message = "Settlement ID cannot be blank.")
        }
        if (settlement.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (settlement.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        }
        if (settlement.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created by cannot be blank.")
        }
        if (settlement.createdAt <= 0) {
            return DomainResult.Error(message = "Created timestamp must be positive.")
        }
        if (settlement.updatedAt < settlement.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot be before created timestamp.")
        }
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Settlement must have at least one product settlement line.")
        }

        // Line-level validation
        for (line in lines) {
            val lineRes = DeliveryPartialSettlementLineValidator.validateLine(line)
            if (lineRes is DomainResult.Error) return lineRes

            if (line.projectId != settlement.projectId) {
                return DomainResult.Error(
                    message = "Project mismatch: Line belongs to '${line.projectId}', but settlement is in '${settlement.projectId}'."
                )
            }
            if (line.settlementId != settlement.settlementId) {
                return DomainResult.Error(
                    message = "Settlement ID mismatch: Line belongs to '${line.settlementId}', but parent settlement is '${settlement.settlementId}'."
                )
            }
        }

        // Check for duplicate DO lines in settlement
        val doLineIds = lines.map { it.deliveryOrderLineId }
        if (doLineIds.size != doLineIds.distinct().size) {
            return DomainResult.Error(message = "Settlement lines contain duplicate Delivery Order Line items.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateDeliveryOrderEligibility(
        order: DeliveryOrder,
        targetProjectId: String
    ): DomainResult<Unit> {
        if (order.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Delivery Order belongs to '${order.projectId}', but target project is '$targetProjectId'."
            )
        }
        if (order.status == com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cannot create or maintain settlement for CANCELLED Delivery Order '${order.deliveryOrderNo}'."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateImmutableIdentity(
        original: DeliveryPartialSettlement,
        updated: DeliveryPartialSettlement
    ): DomainResult<Unit> {
        if (original.settlementId != updated.settlementId) {
            return DomainResult.Error(message = "Settlement ID is immutable.")
        }
        if (original.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable.")
        }
        if (original.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order ID is immutable.")
        }
        if (original.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Created By is immutable.")
        }
        if (original.createdAt != updated.createdAt) {
            return DomainResult.Error(message = "Created At timestamp is immutable.")
        }
        return DomainResult.Success(Unit)
    }
}

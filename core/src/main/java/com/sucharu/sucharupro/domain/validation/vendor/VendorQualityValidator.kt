package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import java.math.BigDecimal
import java.math.RoundingMode

object VendorQualityValidator {

    fun validateInspection(inspection: VendorQualityInspection): DomainResult<Unit> {
        if (inspection.inspectionId.isBlank()) return DomainResult.Error(IllegalArgumentException("Inspection ID cannot be blank"))
        if (inspection.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (inspection.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (inspection.inspectionReference.isBlank()) return DomainResult.Error(IllegalArgumentException("Inspection reference cannot be blank"))

        if (inspection.receivedQuantity < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Received quantity cannot be negative"))
        }
        if (inspection.acceptedQuantity < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Accepted quantity cannot be negative"))
        }
        if (inspection.rejectedQuantity < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Rejected quantity cannot be negative"))
        }
        if (inspection.conditionalQuantity < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Conditional quantity cannot be negative"))
        }

        val totalInspected = inspection.acceptedQuantity + inspection.rejectedQuantity + inspection.conditionalQuantity
        if (totalInspected > inspection.receivedQuantity) {
            return DomainResult.Error(IllegalArgumentException("Sum of accepted ($totalInspected) cannot exceed received quantity (${inspection.receivedQuantity})"))
        }

        // Validate items if present
        for ((idx, item) in inspection.items.withIndex()) {
            val itemErr = validateInspectionItem(item)
            if (itemErr is DomainResult.Error) {
                return DomainResult.Error(IllegalArgumentException("Item at index $idx invalid: ${itemErr.message}"))
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateInspectionItem(item: VendorQualityInspectionItem): DomainResult<Unit> {
        if (item.inspectionItemId.isBlank()) return DomainResult.Error(IllegalArgumentException("Inspection item ID cannot be blank"))
        if (item.itemDescription.isBlank()) return DomainResult.Error(IllegalArgumentException("Item description cannot be blank"))
        if (item.receivedQuantity <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Item received quantity must be greater than zero"))
        }
        if (item.acceptedQuantity < BigDecimal.ZERO || item.rejectedQuantity < BigDecimal.ZERO || item.conditionalQuantity < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Item quantities cannot be negative"))
        }
        val sum = item.acceptedQuantity + item.rejectedQuantity + item.conditionalQuantity
        if (sum > item.receivedQuantity) {
            return DomainResult.Error(IllegalArgumentException("Item allocated quantity ($sum) cannot exceed received quantity (${item.receivedQuantity})"))
        }
        if (item.defectCount < 0) {
            return DomainResult.Error(IllegalArgumentException("Defect count cannot be negative"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateInspectionTransition(current: VendorInspectionStatus, target: VendorInspectionStatus): DomainResult<Unit> {
        return if (current.canTransitionTo(target)) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(IllegalStateException("Invalid inspection state transition from $current to $target"))
        }
    }

    fun validateDefect(defect: VendorDefect): DomainResult<Unit> {
        if (defect.defectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Defect ID cannot be blank"))
        if (defect.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (defect.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (defect.inspectionId.isBlank()) return DomainResult.Error(IllegalArgumentException("Inspection ID cannot be blank"))
        if (defect.description.isBlank()) return DomainResult.Error(IllegalArgumentException("Defect description cannot be blank"))
        if (defect.quantityAffected < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Quantity affected cannot be negative"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateRejection(rejection: VendorRejection): DomainResult<Unit> {
        if (rejection.rejectionId.isBlank()) return DomainResult.Error(IllegalArgumentException("Rejection ID cannot be blank"))
        if (rejection.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (rejection.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (rejection.rejectionReference.isBlank()) return DomainResult.Error(IllegalArgumentException("Rejection reference cannot be blank"))
        if (rejection.rejectionReason.isBlank()) return DomainResult.Error(IllegalArgumentException("Rejection reason cannot be blank"))
        if (rejection.rejectedQuantity <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Rejected quantity must be greater than zero"))
        }
        if (rejection.rejectedValue.amount < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Rejected value cannot be negative"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateRejectionTransition(current: VendorRejectionStatus, target: VendorRejectionStatus): DomainResult<Unit> {
        return if (current.canTransitionTo(target)) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(IllegalStateException("Invalid rejection state transition from $current to $target"))
        }
    }

    fun validateDispute(dispute: VendorDispute): DomainResult<Unit> {
        if (dispute.disputeId.isBlank()) return DomainResult.Error(IllegalArgumentException("Dispute ID cannot be blank"))
        if (dispute.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (dispute.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (dispute.disputeReference.isBlank()) return DomainResult.Error(IllegalArgumentException("Dispute reference cannot be blank"))
        if (dispute.subject.isBlank()) return DomainResult.Error(IllegalArgumentException("Dispute subject cannot be blank"))
        if (dispute.description.isBlank()) return DomainResult.Error(IllegalArgumentException("Dispute description cannot be blank"))
        if (dispute.raisedBy.isBlank()) return DomainResult.Error(IllegalArgumentException("Dispute raisedBy cannot be blank"))
        if (dispute.disputedQuantity < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Disputed quantity cannot be negative"))
        }
        if (dispute.disputedAmount.amount < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Disputed amount cannot be negative"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateDisputeTransition(current: VendorDisputeStatus, target: VendorDisputeStatus): DomainResult<Unit> {
        return if (current.canTransitionTo(target)) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(IllegalStateException("Invalid dispute state transition from $current to $target"))
        }
    }

    /**
     * Compute defect rate safely with deterministic rounding.
     */
    fun computeDefectRate(defectQty: BigDecimal, inspectedQty: BigDecimal): BigDecimal {
        if (inspectedQty <= BigDecimal.ZERO) return BigDecimal.ZERO
        return defectQty.divide(inspectedQty, 4, RoundingMode.HALF_UP)
    }

    /**
     * Compute acceptance rate safely with deterministic rounding.
     */
    fun computeAcceptanceRate(acceptedQty: BigDecimal, receivedQty: BigDecimal): BigDecimal {
        if (receivedQty <= BigDecimal.ZERO) return BigDecimal.ZERO
        return acceptedQty.divide(receivedQty, 4, RoundingMode.HALF_UP)
    }

    /**
     * Compute rejection rate safely with deterministic rounding.
     */
    fun computeRejectionRate(rejectedQty: BigDecimal, receivedQty: BigDecimal): BigDecimal {
        if (receivedQty <= BigDecimal.ZERO) return BigDecimal.ZERO
        return rejectedQty.divide(receivedQty, 4, RoundingMode.HALF_UP)
    }
}

package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlement
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlementAllocation
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus
import java.math.BigDecimal

/**
 * Domain validator for Vendor Settlement operations & Separation of Duties (Module 12 Step 10).
 */
object VendorSettlementValidator {

    fun validateSettlementCreation(
        vendorId: String,
        settlementNumber: String,
        totalAmount: Money,
        allocations: List<VendorSettlementAllocation>
    ): DomainResult<Unit> {
        if (vendorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        }
        if (settlementNumber.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Settlement number cannot be blank"))
        }
        if (totalAmount.isNegative() || totalAmount.isZero()) {
            return DomainResult.Error(IllegalArgumentException("Settlement total amount must be strictly positive"))
        }
        if (allocations.isEmpty()) {
            return DomainResult.Error(IllegalArgumentException("Settlement must have at least one allocation"))
        }

        var sumAllocations = Money.ZERO
        for (alloc in allocations) {
            if (alloc.payableId.isBlank()) {
                return DomainResult.Error(IllegalArgumentException("Allocation payable ID cannot be blank"))
            }
            if (alloc.allocatedAmount.isNegative() || alloc.allocatedAmount.isZero()) {
                return DomainResult.Error(IllegalArgumentException("Allocated amount must be strictly positive"))
            }
            sumAllocations = sumAllocations + alloc.allocatedAmount
        }

        if (sumAllocations != totalAmount) {
            return DomainResult.Error(IllegalArgumentException("Sum of allocations (${sumAllocations.amount.toPlainString()}) does not match settlement total (${totalAmount.amount.toPlainString()})"))
        }

        return DomainResult.Success(Unit)
    }

    fun validateSeparationOfDuties(
        settlement: VendorSettlement,
        approverId: String
    ): DomainResult<Unit> {
        if (settlement.createdBy == approverId) {
            return DomainResult.Error(
                IllegalStateException("Separation of Duties violation: Settlement creator '${settlement.createdBy}' cannot approve their own settlement")
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        currentStatus: VendorSettlementStatus,
        targetStatus: VendorSettlementStatus
    ): DomainResult<Unit> {
        val validTransitions = when (currentStatus) {
            VendorSettlementStatus.DRAFT -> setOf(
                VendorSettlementStatus.ELIGIBLE,
                VendorSettlementStatus.APPROVED,
                VendorSettlementStatus.CANCELLED
            )
            VendorSettlementStatus.ELIGIBLE -> setOf(
                VendorSettlementStatus.APPROVED,
                VendorSettlementStatus.REJECTED,
                VendorSettlementStatus.CANCELLED
            )
            VendorSettlementStatus.APPROVED -> setOf(
                VendorSettlementStatus.PROCESSING,
                VendorSettlementStatus.CANCELLED
            )
            VendorSettlementStatus.PROCESSING -> setOf(
                VendorSettlementStatus.SETTLED,
                VendorSettlementStatus.FAILED,
                VendorSettlementStatus.RECONCILIATION_REQUIRED
            )
            VendorSettlementStatus.RECONCILIATION_REQUIRED -> setOf(
                VendorSettlementStatus.SETTLED,
                VendorSettlementStatus.FAILED
            )
            VendorSettlementStatus.SETTLED,
            VendorSettlementStatus.REJECTED,
            VendorSettlementStatus.CANCELLED,
            VendorSettlementStatus.FAILED -> emptySet()
        }

        if (targetStatus !in validTransitions) {
            return DomainResult.Error(
                IllegalStateException("Invalid state transition from $currentStatus to $targetStatus")
            )
        }
        return DomainResult.Success(Unit)
    }
}

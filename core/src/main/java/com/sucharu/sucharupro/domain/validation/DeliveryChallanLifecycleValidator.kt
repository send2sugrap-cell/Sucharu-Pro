package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus

/**
 * Validator for Delivery Challan lifecycle transitions (Module 08 Step 02).
 */
object DeliveryChallanLifecycleValidator {

    fun validateTransition(
        currentStatus: DeliveryChallanStatus,
        targetStatus: DeliveryChallanStatus
    ): DomainResult<Unit> {
        return when (currentStatus) {
            DeliveryChallanStatus.DRAFT -> {
                if (targetStatus == DeliveryChallanStatus.PENDING || targetStatus == DeliveryChallanStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Draft challan can only transition to Pending or Cancelled.")
                }
            }
            DeliveryChallanStatus.PENDING -> {
                if (targetStatus == DeliveryChallanStatus.APPROVED ||
                    targetStatus == DeliveryChallanStatus.CANCELLED ||
                    targetStatus == DeliveryChallanStatus.DRAFT) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Pending challan can transition to Approved, Cancelled, or back to Draft.")
                }
            }
            DeliveryChallanStatus.APPROVED -> {
                if (targetStatus == DeliveryChallanStatus.READY_FOR_DISPATCH || targetStatus == DeliveryChallanStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Approved challan can transition to Ready for Dispatch or Cancelled.")
                }
            }
            DeliveryChallanStatus.READY_FOR_DISPATCH -> {
                if (targetStatus == DeliveryChallanStatus.DISPATCHED || targetStatus == DeliveryChallanStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Ready for Dispatch challan can transition to Dispatched or Cancelled.")
                }
            }
            DeliveryChallanStatus.DISPATCHED -> {
                if (targetStatus == DeliveryChallanStatus.DELIVERED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Dispatched challan can only transition to Delivered.")
                }
            }
            DeliveryChallanStatus.DELIVERED -> {
                DomainResult.Error(message = "Delivered is a terminal status and cannot be changed.")
            }
            DeliveryChallanStatus.CANCELLED -> {
                DomainResult.Error(message = "Cancelled is a terminal status and cannot be changed.")
            }
        }
    }
}

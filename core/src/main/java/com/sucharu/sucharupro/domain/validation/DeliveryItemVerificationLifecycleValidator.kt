package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus

/**
 * Validates lifecycle state transitions for Delivery Item Verifications (Module 08 Step 04).
 */
object DeliveryItemVerificationLifecycleValidator {

    fun validateTransition(
        currentStatus: DeliveryItemVerificationStatus,
        targetStatus: DeliveryItemVerificationStatus
    ): DomainResult<Unit> {
        return when (currentStatus) {
            DeliveryItemVerificationStatus.DRAFT -> {
                if (targetStatus == DeliveryItemVerificationStatus.PENDING ||
                    targetStatus == DeliveryItemVerificationStatus.IN_PROGRESS ||
                    targetStatus == DeliveryItemVerificationStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Draft verification can only transition to Pending, In Progress, or Cancelled.")
                }
            }
            DeliveryItemVerificationStatus.PENDING -> {
                if (targetStatus == DeliveryItemVerificationStatus.IN_PROGRESS ||
                    targetStatus == DeliveryItemVerificationStatus.DRAFT ||
                    targetStatus == DeliveryItemVerificationStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Pending verification can transition to In Progress, Draft, or Cancelled.")
                }
            }
            DeliveryItemVerificationStatus.IN_PROGRESS -> {
                if (targetStatus == DeliveryItemVerificationStatus.VERIFIED ||
                    targetStatus == DeliveryItemVerificationStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "In Progress verification can transition to Verified or Cancelled.")
                }
            }
            DeliveryItemVerificationStatus.VERIFIED -> {
                if (targetStatus == DeliveryItemVerificationStatus.CLOSED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Verified delivery can only transition to Closed.")
                }
            }
            DeliveryItemVerificationStatus.CLOSED -> {
                DomainResult.Error(message = "Closed is a terminal status and cannot be changed.")
            }
            DeliveryItemVerificationStatus.CANCELLED -> {
                DomainResult.Error(message = "Cancelled is a terminal status and cannot be changed.")
            }
        }
    }
}

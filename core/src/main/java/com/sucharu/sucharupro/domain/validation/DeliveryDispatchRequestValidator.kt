package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DispatchRequestStatus

/**
 * Domain validator for Delivery Dispatch Requests (Module 08 Step 01).
 */
object DeliveryDispatchRequestValidator {

    /**
     * Validates structural invariants of a [DeliveryDispatchRequest].
     */
    fun validateDispatchRequest(request: DeliveryDispatchRequest): DomainResult<Unit> {
        if (request.dispatchRequestId.isBlank()) {
            return DomainResult.Error(message = "Dispatch Request ID cannot be blank.")
        }
        if (request.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (request.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        }
        if (request.requestedBy.isBlank()) {
            return DomainResult.Error(message = "Requested By cannot be blank.")
        }
        if (request.requestedAt <= 0) {
            return DomainResult.Error(message = "Requested At timestamp must be positive.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates eligibility of a Delivery Order to have a dispatch request created.
     * Only orders in APPROVED or READY_FOR_DISPATCH can be requested for dispatch.
     */
    fun validateEligibilityForDispatch(orderStatus: DeliveryOrderStatus): DomainResult<Unit> {
        return when (orderStatus) {
            DeliveryOrderStatus.APPROVED,
            DeliveryOrderStatus.READY_FOR_DISPATCH -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Cannot create dispatch request for Delivery Order in '$orderStatus' status. Order must be APPROVED or READY_FOR_DISPATCH."
            )
        }
    }

    /**
     * Validates dispatch request lifecycle transitions.
     */
    fun validateStatusTransition(
        currentStatus: DispatchRequestStatus,
        targetStatus: DispatchRequestStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        return when (currentStatus) {
            DispatchRequestStatus.REQUESTED -> {
                if (targetStatus == DispatchRequestStatus.APPROVED || targetStatus == DispatchRequestStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Requested dispatch can only transition to Approved or Cancelled.")
                }
            }
            DispatchRequestStatus.APPROVED -> {
                if (targetStatus == DispatchRequestStatus.READY || targetStatus == DispatchRequestStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Approved dispatch request can transition to Ready or Cancelled.")
                }
            }
            DispatchRequestStatus.READY -> {
                if (targetStatus == DispatchRequestStatus.DISPATCHED || targetStatus == DispatchRequestStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Ready dispatch request can transition to Dispatched or Cancelled.")
                }
            }
            DispatchRequestStatus.DISPATCHED -> {
                DomainResult.Error(message = "Dispatched is a terminal dispatch status and cannot be changed.")
            }
            DispatchRequestStatus.CANCELLED -> {
                DomainResult.Error(message = "Cancelled dispatch request cannot be updated.")
            }
        }
    }
}

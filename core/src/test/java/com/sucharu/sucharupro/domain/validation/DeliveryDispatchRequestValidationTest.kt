package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.DispatchRequestStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryDispatchRequestValidationTest {

    private fun createValidRequest(
        requestId: String = "REQ-01",
        projectId: String = "PRJ-01",
        orderId: String = "DO-01",
        requestedBy: String = "user-1",
        requestedAt: Long = 1000L,
        priority: DeliveryPriority = DeliveryPriority.NORMAL,
        status: DispatchRequestStatus = DispatchRequestStatus.REQUESTED
    ): DeliveryDispatchRequest {
        return DeliveryDispatchRequest(
            dispatchRequestId = requestId,
            projectId = projectId,
            deliveryOrderId = orderId,
            requestedBy = requestedBy,
            requestedAt = requestedAt,
            priority = priority,
            status = status,
            notes = "Standard dispatch"
        )
    }

    @Test
    fun `valid dispatch request passes validation`() {
        val request = createValidRequest()
        val result = DeliveryDispatchRequestValidator.validateDispatchRequest(request)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `order eligibility succeeds for APPROVED and READY_FOR_DISPATCH`() {
        assertTrue(
            DeliveryDispatchRequestValidator.validateEligibilityForDispatch(DeliveryOrderStatus.APPROVED) is DomainResult.Success
        )
        assertTrue(
            DeliveryDispatchRequestValidator.validateEligibilityForDispatch(DeliveryOrderStatus.READY_FOR_DISPATCH) is DomainResult.Success
        )
    }

    @Test
    fun `order eligibility fails for DRAFT, PENDING, CANCELLED, DELIVERED`() {
        assertTrue(
            DeliveryDispatchRequestValidator.validateEligibilityForDispatch(DeliveryOrderStatus.DRAFT) is DomainResult.Error
        )
        assertTrue(
            DeliveryDispatchRequestValidator.validateEligibilityForDispatch(DeliveryOrderStatus.PENDING) is DomainResult.Error
        )
        assertTrue(
            DeliveryDispatchRequestValidator.validateEligibilityForDispatch(DeliveryOrderStatus.CANCELLED) is DomainResult.Error
        )
        assertTrue(
            DeliveryDispatchRequestValidator.validateEligibilityForDispatch(DeliveryOrderStatus.DELIVERED) is DomainResult.Error
        )
    }

    @Test
    fun `dispatch request status transitions follow correct lifecycle`() {
        assertTrue(
            DeliveryDispatchRequestValidator.validateStatusTransition(
                DispatchRequestStatus.REQUESTED,
                DispatchRequestStatus.APPROVED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryDispatchRequestValidator.validateStatusTransition(
                DispatchRequestStatus.APPROVED,
                DispatchRequestStatus.READY
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryDispatchRequestValidator.validateStatusTransition(
                DispatchRequestStatus.READY,
                DispatchRequestStatus.DISPATCHED
            ) is DomainResult.Success
        )
        assertTrue(
            DeliveryDispatchRequestValidator.validateStatusTransition(
                DispatchRequestStatus.DISPATCHED,
                DispatchRequestStatus.READY
            ) is DomainResult.Error
        )
    }
}

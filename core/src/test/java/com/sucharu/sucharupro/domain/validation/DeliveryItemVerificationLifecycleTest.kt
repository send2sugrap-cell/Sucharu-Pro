package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryItemVerificationLifecycleTest {

    @Test
    fun `valid progression from draft through closed succeeds`() {
        // DRAFT -> PENDING
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.DRAFT,
                DeliveryItemVerificationStatus.PENDING
            ) is DomainResult.Success
        )

        // PENDING -> IN_PROGRESS
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.PENDING,
                DeliveryItemVerificationStatus.IN_PROGRESS
            ) is DomainResult.Success
        )

        // IN_PROGRESS -> VERIFIED
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.IN_PROGRESS,
                DeliveryItemVerificationStatus.VERIFIED
            ) is DomainResult.Success
        )

        // VERIFIED -> CLOSED
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.VERIFIED,
                DeliveryItemVerificationStatus.CLOSED
            ) is DomainResult.Success
        )
    }

    @Test
    fun `direct jump from draft to in progress succeeds`() {
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.DRAFT,
                DeliveryItemVerificationStatus.IN_PROGRESS
            ) is DomainResult.Success
        )
    }

    @Test
    fun `illegal state jumps are rejected`() {
        // DRAFT -> CLOSED
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.DRAFT,
                DeliveryItemVerificationStatus.CLOSED
            ) is DomainResult.Error
        )
        // IN_PROGRESS -> CLOSED
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.IN_PROGRESS,
                DeliveryItemVerificationStatus.CLOSED
            ) is DomainResult.Error
        )
    }

    @Test
    fun `transitions from terminal states are rejected`() {
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.CLOSED,
                DeliveryItemVerificationStatus.DRAFT
            ) is DomainResult.Error
        )
        assertTrue(
            DeliveryItemVerificationLifecycleValidator.validateTransition(
                DeliveryItemVerificationStatus.CANCELLED,
                DeliveryItemVerificationStatus.VERIFIED
            ) is DomainResult.Error
        )
    }
}

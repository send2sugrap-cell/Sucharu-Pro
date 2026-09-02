package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReconciliationLifecycleTest {

    @Test
    fun `open to in progress transition is valid`() {
        val res = DeliveryReconciliationLifecycleValidator.validateTransition(
            DeliveryReconciliationStatus.OPEN,
            DeliveryReconciliationStatus.IN_PROGRESS
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `in progress to reconciled transition is valid`() {
        val res = DeliveryReconciliationLifecycleValidator.validateTransition(
            DeliveryReconciliationStatus.IN_PROGRESS,
            DeliveryReconciliationStatus.RECONCILED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `in progress to disputed transition is valid`() {
        val res = DeliveryReconciliationLifecycleValidator.validateTransition(
            DeliveryReconciliationStatus.IN_PROGRESS,
            DeliveryReconciliationStatus.DISPUTED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `disputed to resolved transition is valid`() {
        val res = DeliveryReconciliationLifecycleValidator.validateTransition(
            DeliveryReconciliationStatus.DISPUTED,
            DeliveryReconciliationStatus.RESOLVED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `reconciled to closed transition is valid`() {
        val res = DeliveryReconciliationLifecycleValidator.validateTransition(
            DeliveryReconciliationStatus.RECONCILED,
            DeliveryReconciliationStatus.CLOSED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `terminal closed status rejects any transition`() {
        val res = DeliveryReconciliationLifecycleValidator.validateTransition(
            DeliveryReconciliationStatus.CLOSED,
            DeliveryReconciliationStatus.OPEN
        )
        assertTrue(res is DomainResult.Error)
    }
}

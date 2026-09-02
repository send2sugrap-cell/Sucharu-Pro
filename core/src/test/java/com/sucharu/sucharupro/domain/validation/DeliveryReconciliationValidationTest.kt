package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReconciliationValidationTest {

    private fun sampleReconciliation(
        id: String = "REC-01",
        projectId: String = "PRJ-01",
        orderId: String = "DO-01",
        ordered: Double = 100.0,
        delivered: Double = 100.0
    ) = DeliveryReconciliation(
        reconciliationId = id,
        projectId = projectId,
        deliveryOrderId = orderId,
        orderedQuantity = ordered,
        deliveredQuantity = delivered,
        reconciliationStatus = DeliveryReconciliationStatus.OPEN,
        createdBy = "user-1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun sampleItem(
        itemId: String = "ITEM-01",
        recId: String = "REC-01",
        projectId: String = "PRJ-01"
    ) = DeliveryReconciliationItem(
        reconciliationItemId = itemId,
        reconciliationId = recId,
        projectId = projectId,
        deliveryOrderLineId = "DOL-01",
        productId = "PROD-01",
        orderedQuantity = 100.0,
        deliveredQuantity = 100.0,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun `valid reconciliation passes validation`() {
        val rec = sampleReconciliation()
        val item = sampleItem()
        val result = DeliveryReconciliationValidator.validateReconciliation(rec, listOf(item), "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `blank reconciliation id fails construction`() {
        var threw = false
        try {
            sampleReconciliation(id = "  ")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `project mismatch fails validation`() {
        val rec = sampleReconciliation(projectId = "PRJ-02")
        val item = sampleItem(projectId = "PRJ-02")
        val result = DeliveryReconciliationValidator.validateReconciliation(rec, listOf(item), "PRJ-01")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `negative quantities in items fail instantiation`() {
        var threwException = false
        try {
            DeliveryReconciliationItem(
                reconciliationItemId = "ITEM-NEG",
                reconciliationId = "REC-01",
                projectId = "PRJ-01",
                deliveryOrderLineId = "DOL-01",
                productId = "PROD-01",
                orderedQuantity = -10.0,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        } catch (e: IllegalArgumentException) {
            threwException = true
        }
        assertTrue(threwException)
    }

    @Test
    fun `validateImmutableIdentity rejects mutating closed reconciliation`() {
        val closed = sampleReconciliation().copy(reconciliationStatus = DeliveryReconciliationStatus.CLOSED)
        val reopened = closed.copy(reconciliationStatus = DeliveryReconciliationStatus.OPEN)
        val result = DeliveryReconciliationValidator.validateImmutableIdentity(closed, reopened)
        assertTrue(result is DomainResult.Error)
    }
}

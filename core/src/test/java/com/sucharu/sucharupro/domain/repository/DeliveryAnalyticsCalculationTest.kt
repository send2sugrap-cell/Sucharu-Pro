package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.service.delivery.DeliveryAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryAnalyticsCalculationTest {

    @Test
    fun `partial and discrepancy rates calculate proportionally`() {
        val reconciliations = listOf(
            DeliveryReconciliation(
                reconciliationId = "REC-1",
                projectId = "PRJ-01",
                deliveryOrderId = "DO-1",
                orderedQuantity = 200.0,
                dispatchedQuantity = 200.0,
                deliveredQuantity = 150.0,
                acceptedPodQuantity = 120.0,
                returnedQuantity = 20.0,
                outstandingQuantity = 30.0,
                discrepancyQuantity = 30.0,
                reconciliationStatus = DeliveryReconciliationStatus.REQUIRES_REVIEW,
                createdBy = "u1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val summary = DeliveryAnalyticsCalculator.calculateSummary(
            projectId = "PRJ-01",
            reconciliations = reconciliations
        )

        assertEquals(75.0, summary.deliverySuccessRate, 0.001) // 150 / 200 * 100
        assertEquals(80.0, summary.podAcceptanceRate, 0.001) // 120 / 150 * 100
        assertEquals(10.0, summary.returnRate, 0.001) // 20 / 200 * 100
        assertEquals(15.0, summary.discrepancyRate, 0.001) // 30 / 200 * 100
    }
}

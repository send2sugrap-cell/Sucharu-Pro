package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.service.delivery.DeliveryAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryAnalyticsSummaryTest {

    @Test
    fun `empty delivery data produces zero counts and rates`() {
        val summary = DeliveryAnalyticsCalculator.calculateSummary(projectId = "PRJ-01")
        assertEquals(0, summary.totalDeliveryOrders)
        assertEquals(0, summary.totalShipments)
        assertEquals(0.0, summary.deliverySuccessRate, 0.001)
        assertEquals(0.0, summary.podAcceptanceRate, 0.001)
        assertEquals(0.0, summary.returnRate, 0.001)
        assertEquals(0.0, summary.discrepancyRate, 0.001)
    }

    @Test
    fun `summary accurately aggregates quantities and calculates rates`() {
        val orders = listOf(
            DeliveryOrder(
                deliveryOrderId = "DO-1",
                projectId = "PRJ-01",
                deliveryOrderNo = "DON-1",
                customerId = "CUST-1",
                sourceReferenceId = "SO-1",
                sourceReferenceType = "SO",
                deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
                priority = DeliveryPriority.NORMAL,
                status = DeliveryOrderStatus.DELIVERED,
                requestedDeliveryDate = 2000L,
                notes = null,
                createdBy = "u1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        val lines = listOf(DeliveryOrderLine("DOL-1", "DO-1", "PRJ-01", "P-1", 100.0, null))
        val reconciliations = listOf(
            DeliveryReconciliation(
                reconciliationId = "REC-1",
                projectId = "PRJ-01",
                deliveryOrderId = "DO-1",
                orderedQuantity = 100.0,
                dispatchedQuantity = 100.0,
                deliveredQuantity = 100.0,
                acceptedPodQuantity = 100.0,
                returnedQuantity = 0.0,
                outstandingQuantity = 0.0,
                discrepancyQuantity = 0.0,
                reconciliationStatus = DeliveryReconciliationStatus.RECONCILED,
                createdBy = "u1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val summary = DeliveryAnalyticsCalculator.calculateSummary(
            projectId = "PRJ-01",
            orders = orders,
            orderLines = lines,
            reconciliations = reconciliations
        )

        assertEquals(1, summary.totalDeliveryOrders)
        assertEquals(100.0, summary.totalOrderedQuantity, 0.001)
        assertEquals(100.0, summary.totalDeliveredQuantity, 0.001)
        assertEquals(100.0, summary.deliverySuccessRate, 0.001)
        assertEquals(100.0, summary.podAcceptanceRate, 0.001)
        assertEquals(0.0, summary.returnRate, 0.001)
        assertEquals(0.0, summary.discrepancyRate, 0.001)
    }
}

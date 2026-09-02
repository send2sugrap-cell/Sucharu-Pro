package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryAnalyticsReconciliationIntegrationTest {

    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var governanceDataSource: FakeDeliveryGovernanceDataSource
    private lateinit var repository: DeliveryAnalyticsRepository

    @Before
    fun setUp() {
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        governanceDataSource = FakeDeliveryGovernanceDataSource()
        repository = DeliveryAnalyticsRepositoryImpl(
            governanceDataSource = governanceDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource,
            reconciliationDataSource = reconciliationDataSource
        )
    }

    @Test
    fun `reconciliation quantities drive authoritative reconciliation summary values`() = runBlocking {
        val order = DeliveryOrder(
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
        val line = DeliveryOrderLine("DOL-1", "DO-1", "PRJ-01", "P-1", 500.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))

        val reconciliation = DeliveryReconciliation(
            reconciliationId = "REC-1",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-1",
            orderedQuantity = 500.0,
            dispatchedQuantity = 500.0,
            deliveredQuantity = 450.0,
            acceptedPodQuantity = 400.0,
            returnedQuantity = 30.0,
            outstandingQuantity = 20.0,
            discrepancyQuantity = 20.0,
            reconciliationStatus = DeliveryReconciliationStatus.REQUIRES_REVIEW,
            createdBy = "u1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val recItem = DeliveryReconciliationItem(
            reconciliationItemId = "RECI-1",
            reconciliationId = "REC-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "P-1",
            orderedQuantity = 500.0,
            dispatchedQuantity = 500.0,
            deliveredQuantity = 450.0,
            acceptedPodQuantity = 400.0,
            returnedQuantity = 30.0,
            outstandingQuantity = 20.0,
            discrepancyQuantity = 20.0
        )
        reconciliationDataSource.insertReconciliation(reconciliation, listOf(recItem))

        val result = repository.getSummary(DeliveryAnalyticsFilter(projectId = "PRJ-01"), UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
        val summary = (result as DomainResult.Success).data
        assertEquals(500.0, summary.totalOrderedQuantity, 0.001)
        assertEquals(450.0, summary.totalDeliveredQuantity, 0.001)
        assertEquals(20.0, summary.totalDiscrepancyQuantity, 0.001)
        assertEquals(1, summary.totalDiscrepancies)
    }
}

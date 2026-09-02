package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryAnalyticsDateBoundaryTest {

    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var governanceDataSource: FakeDeliveryGovernanceDataSource
    private lateinit var repository: DeliveryAnalyticsRepository

    @Before
    fun setUp() {
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        governanceDataSource = FakeDeliveryGovernanceDataSource()
        repository = DeliveryAnalyticsRepositoryImpl(
            governanceDataSource = governanceDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource
        )
    }

    @Test
    fun `inclusive date boundaries filter records strictly`() = runBlocking {
        val order1 = DeliveryOrder(
            deliveryOrderId = "DO-Early",
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
            createdAt = 500L,
            updatedAt = 500L
        )
        val line1 = DeliveryOrderLine("DOL-1", "DO-Early", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order1, listOf(line1))

        val order2 = DeliveryOrder(
            deliveryOrderId = "DO-ExactMin",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-2",
            customerId = "CUST-1",
            sourceReferenceId = "SO-2",
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
        val line2 = DeliveryOrderLine("DOL-2", "DO-ExactMin", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order2, listOf(line2))

        val order3 = DeliveryOrder(
            deliveryOrderId = "DO-ExactMax",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-3",
            customerId = "CUST-1",
            sourceReferenceId = "SO-3",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DELIVERED,
            requestedDeliveryDate = 2000L,
            notes = null,
            createdBy = "u1",
            createdAt = 2000L,
            updatedAt = 2000L
        )
        val line3 = DeliveryOrderLine("DOL-3", "DO-ExactMax", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order3, listOf(line3))

        val order4 = DeliveryOrder(
            deliveryOrderId = "DO-Late",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-4",
            customerId = "CUST-1",
            sourceReferenceId = "SO-4",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DELIVERED,
            requestedDeliveryDate = 2500L,
            notes = null,
            createdBy = "u1",
            createdAt = 3000L,
            updatedAt = 3000L
        )
        val line4 = DeliveryOrderLine("DOL-4", "DO-Late", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order4, listOf(line4))

        val result = repository.getSummary(
            DeliveryAnalyticsFilter(
                projectId = "PRJ-01",
                dateFrom = 1000L,
                dateTo = 2000L
            ),
            UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val summary = (result as DomainResult.Success).data
        assertEquals(2, summary.totalDeliveryOrders) // exactMin & exactMax
    }
}

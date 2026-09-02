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

class DeliveryAnalyticsProjectIsolationTest {

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
    fun `analytics query strictly isolates project data`() = runBlocking {
        val orderA = DeliveryOrder(
            deliveryOrderId = "DO-A",
            projectId = "PRJ-A",
            deliveryOrderNo = "DON-A",
            customerId = "CUST-A",
            sourceReferenceId = "SO-A",
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
        val lineA = DeliveryOrderLine("DOL-A", "DO-A", "PRJ-A", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(orderA, listOf(lineA))

        val orderB = DeliveryOrder(
            deliveryOrderId = "DO-B",
            projectId = "PRJ-B",
            deliveryOrderNo = "DON-B",
            customerId = "CUST-B",
            sourceReferenceId = "SO-B",
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
        val lineB = DeliveryOrderLine("DOL-B", "DO-B", "PRJ-B", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(orderB, listOf(lineB))

        val resultA = repository.getSummary(
            DeliveryAnalyticsFilter(projectId = "PRJ-A"),
            UserRole.ADMIN
        )
        val resultB = repository.getSummary(
            DeliveryAnalyticsFilter(projectId = "PRJ-B"),
            UserRole.ADMIN
        )

        assertTrue(resultA is DomainResult.Success)
        assertTrue(resultB is DomainResult.Success)
        assertEquals(1, (resultA as DomainResult.Success).data.totalDeliveryOrders)
        assertEquals(1, (resultB as DomainResult.Success).data.totalDeliveryOrders)
    }
}

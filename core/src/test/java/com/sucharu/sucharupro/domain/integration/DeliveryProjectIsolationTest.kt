package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryProjectIsolationTest {

    private lateinit var dataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryOrderRepository

    @Before
    fun setUp() {
        dataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryOrderRepositoryImpl(dataSource)
    }

    @Test
    fun `project isolation prevents cross project data access and mutation`() = runBlocking {
        val orderA = DeliveryOrder(
            deliveryOrderId = "DO-PRJ-A",
            projectId = "PRJ-A",
            deliveryOrderNo = "DEL-A-01",
            customerId = "CUST-A",
            sourceReferenceId = null,
            sourceReferenceType = null,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = 10000L,
            notes = null,
            createdBy = "user-a",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val lineA = DeliveryOrderLine(
            lineId = "LINE-A",
            deliveryOrderId = "DO-PRJ-A",
            projectId = "PRJ-A",
            productId = "PROD-A",
            requestedQuantity = 5.0,
            notes = null
        )

        repository.createDeliveryOrder(orderA, listOf(lineA), UserRole.ADMIN, "PRJ-A")

        // 1. User from PRJ-B querying order from PRJ-A must be denied
        val getRes = repository.getDeliveryOrder("DO-PRJ-A", UserRole.MANAGER, callerProjectId = "PRJ-B")
        assertTrue(getRes is DomainResult.Error)
        assertTrue((getRes as DomainResult.Error).message.contains("Access denied"))

        // 2. User from PRJ-B submitting order from PRJ-A must be denied
        val submitRes = repository.submitDeliveryOrder("DO-PRJ-A", "user-b", UserRole.MANAGER, callerProjectId = "PRJ-B")
        assertTrue(submitRes is DomainResult.Error)

        // 3. User from PRJ-B approving order from PRJ-A must be denied
        val approveRes = repository.approveDeliveryOrder("DO-PRJ-A", "user-b", UserRole.MANAGER, callerProjectId = "PRJ-B")
        assertTrue(approveRes is DomainResult.Error)

        // 4. Reactive query for PRJ-B returns 0 orders
        val listB = repository.observeDeliveryOrders("PRJ-B").first()
        assertEquals(0, listB.size)

        // 5. Reactive query for PRJ-A returns 1 order
        val listA = repository.observeDeliveryOrders("PRJ-A").first()
        assertEquals(1, listA.size)
    }
}

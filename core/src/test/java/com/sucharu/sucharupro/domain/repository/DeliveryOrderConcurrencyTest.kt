package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryOrderConcurrencyTest {

    private lateinit var dataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryOrderRepository

    @Before
    fun setUp() {
        dataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryOrderRepositoryImpl(dataSource)
    }

    @Test
    fun `concurrent approval calls result in exactly one successful state transition`() = runBlocking {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-CONCUR-1",
            projectId = "PRJ-01",
            deliveryOrderNo = "DEL-CONCUR-01",
            customerId = "CUST-1",
            sourceReferenceId = null,
            sourceReferenceType = null,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = 20000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 10000L,
            updatedAt = 10000L
        )
        val line = DeliveryOrderLine(
            lineId = "LINE-CONCUR-1",
            deliveryOrderId = "DO-CONCUR-1",
            projectId = "PRJ-01",
            productId = "PROD-1",
            requestedQuantity = 10.0,
            notes = null
        )

        repository.createDeliveryOrder(order, listOf(line), UserRole.ADMIN)
        repository.submitDeliveryOrder(order.deliveryOrderId, "user-1", UserRole.ADMIN)

        // Run 10 concurrent approval attempts on the same pending order
        val results = (1..10).map { i ->
            async(Dispatchers.IO) {
                repository.approveDeliveryOrder(order.deliveryOrderId, "approver-$i", UserRole.ADMIN)
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val finalOrder = repository.getDeliveryOrder(order.deliveryOrderId, UserRole.ADMIN)
        assertTrue(finalOrder is DomainResult.Success)
        assertEquals(DeliveryOrderStatus.APPROVED, (finalOrder as DomainResult.Success).data.status)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReturnRepositoryTest {

    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReturnRepository

    @Before
    fun setUp() {
        runBlocking {
            returnDataSource = FakeDeliveryReturnDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            repository = DeliveryReturnRepositoryImpl(returnDataSource, doDataSource)

            val doOrder = DeliveryOrder("DO-RET", "PRJ-01", "DON-RET", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-RET", "DO-RET", "PRJ-01", "PROD-01", 100.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))
        }
    }

    @Test
    fun `full lifecycle from creation to completion succeeds`() = runBlocking {
        // 1. Create Return
        val ret = DeliveryReturn("RET-1", "PRJ-01", "RN-01", "DO-RET", status = DeliveryReturnStatus.DRAFT, requestedBy = "user-1", createdAt = 1000L, updatedAt = 1000L)
        val line = DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-RET",
            productId = "PROD-01",
            returnedQuantity = 50.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val createRes = repository.createReturn(ret, listOf(line), "user-1", UserRole.STAFF)
        assertTrue(createRes is DomainResult.Success)

        // 2. Submit
        val submitRes = repository.submitReturn("RET-1", "user-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)

        // 3. Approve
        val approveRes = repository.approveReturn("RET-1", "mgr", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)

        // 4. Start Receiving & Receive
        repository.startReceiving("RET-1", "wh", UserRole.WAREHOUSE)
        val receiveRes = repository.receiveReturn("RET-1", mapOf("RL-1" to 50.0), "wh", UserRole.WAREHOUSE)
        assertTrue(receiveRes is DomainResult.Success)

        // 5. Start Inspection & Inspect Line
        repository.startInspection("RET-1", "wh", UserRole.WAREHOUSE)
        val inspLineRes = repository.inspectReturnLine("RET-1", "RL-1", acceptedQuantity = 45.0, rejectedQuantity = 5.0, condition = DeliveryReturnLineCondition.GOOD, disposition = DeliveryReturnDisposition.RESTOCK, inspectionNotes = "Good batch", actorId = "wh", callerRole = UserRole.WAREHOUSE)
        assertTrue(inspLineRes is DomainResult.Success)

        // 6. Complete Inspection
        val inspCompRes = repository.completeInspection("RET-1", "wh", UserRole.WAREHOUSE)
        assertTrue(inspCompRes is DomainResult.Success)

        // 7. Complete Return
        val completeRes = repository.completeReturn("RET-1", "mgr", UserRole.MANAGER)
        assertTrue(completeRes is DomainResult.Success)
        assertEquals(DeliveryReturnStatus.COMPLETED, (completeRes as DomainResult.Success).data.status)
    }
}

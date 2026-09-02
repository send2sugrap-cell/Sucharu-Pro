package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationRepositoryTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        returnDataSource = FakeDeliveryReturnDataSource()
        proofDataSource = FakeDeliveryProofDataSource()

        repository = DeliveryReconciliationRepositoryImpl(
            reconciliationDataSource = reconciliationDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource,
            shipmentDataSource = shipmentDataSource,
            returnDataSource = returnDataSource,
            proofDataSource = proofDataSource
        )

        val order = DeliveryOrder(
            deliveryOrderId = "DO-01",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-01",
            customerId = "CUST-01",
            sourceReferenceId = "SO-01",
            sourceReferenceType = "SALES_ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.APPROVED,
            requestedDeliveryDate = 2000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryOrderLine(
            lineId = "DOL-01",
            deliveryOrderId = "DO-01",
            projectId = "PRJ-01",
            productId = "PROD-01",
            requestedQuantity = 100.0,
            notes = null
        )
        orderDataSource.insertDeliveryOrder(order, listOf(line))
    }

    @Test
    fun `createReconciliation initializes aggregate and items`() = runBlocking {
        val res = repository.createReconciliation("DO-01", "operator-1", UserRole.WAREHOUSE)
        assertTrue(res is DomainResult.Success)
        val rec = (res as DomainResult.Success).data
        assertEquals("DO-01", rec.deliveryOrderId)
        assertEquals(100.0, rec.orderedQuantity, 0.001)

        val itemsRes = repository.getItems(rec.reconciliationId, UserRole.ADMIN)
        assertTrue(itemsRes is DomainResult.Success)
        val items = (itemsRes as DomainResult.Success).data
        assertEquals(1, items.size)
        assertEquals("PROD-01", items[0].productId)
    }

    @Test
    fun `startReconciliation and closeReconciliation transition status safely`() = runBlocking {
        val createRes = repository.createReconciliation("DO-01", "operator-1", UserRole.WAREHOUSE)
        val recId = (createRes as DomainResult.Success).data.reconciliationId

        val startRes = repository.startReconciliation(recId, "operator-1", UserRole.WAREHOUSE)
        assertTrue(startRes is DomainResult.Success)
        assertEquals(DeliveryReconciliationStatus.IN_PROGRESS, (startRes as DomainResult.Success).data.reconciliationStatus)

        val closeRes = repository.closeReconciliation(recId, "manager-1", "Closed by management", UserRole.MANAGER)
        assertTrue(closeRes is DomainResult.Success)
        assertEquals(DeliveryReconciliationStatus.CLOSED, (closeRes as DomainResult.Success).data.reconciliationStatus)
    }

    @Test
    fun `observeSummary aggregates status counters`() = runBlocking {
        repository.createReconciliation("DO-01", "operator-1", UserRole.WAREHOUSE)

        val summary = repository.observeSummary("PRJ-01").first()
        assertEquals(1, summary.totalReconciliations)
    }
}

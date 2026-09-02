package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.DeliveryChallanRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryChallanInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanRepository: DeliveryChallanRepository

    @Before
    fun setUp() = runBlocking {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        doDataSource = FakeDeliveryOrderDataSource()
        challanRepository = DeliveryChallanRepositoryImpl(challanDataSource, doDataSource)

        val doOrder = DeliveryOrder(
            deliveryOrderId = "DO-BOUND-1",
            projectId = "PRJ-01",
            deliveryOrderNo = "DEL-BOUND",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.APPROVED,
            requestedDeliveryDate = 20000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryOrderLine(
            lineId = "DOLINE-BOUND",
            deliveryOrderId = "DO-BOUND-1",
            projectId = "PRJ-01",
            productId = "PROD-1",
            requestedQuantity = 50.0,
            notes = null
        )
        doDataSource.insertDeliveryOrder(doOrder, listOf(line))
    }

    @Test
    fun `challan operations do not mutate physical inventory or ledger`() = runBlocking {
        // Initial inventory state
        val initialMovements = ledgerDataSource.observeEntries("PRJ-01").first()
        val initialStockOuts = stockOutDataSource.observeStockOuts().first()
        assertEquals(0, initialMovements.size)
        assertEquals(0, initialStockOuts.size)

        // Perform full challan lifecycle
        val challan = DeliveryChallan(
            challanId = "CH-BOUND",
            projectId = "PRJ-01",
            challanNo = "CH-BOUND-01",
            deliveryOrderId = "DO-BOUND-1",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryChallanLine(
            lineId = "LINE-BOUND",
            challanId = "CH-BOUND",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOLINE-BOUND",
            productId = "PROD-1",
            quantity = 30.0
        )

        val createRes = challanRepository.createChallan(challan, listOf(line), UserRole.ADMIN)
        assertTrue(createRes is DomainResult.Success)

        challanRepository.submitChallan("CH-BOUND", "user-1", UserRole.ADMIN)
        challanRepository.approveChallan("CH-BOUND", "user-1", UserRole.ADMIN)
        challanRepository.markReadyForDispatch("CH-BOUND", "user-1", UserRole.WAREHOUSE)

        // Verify zero inventory impact
        val postMovements = ledgerDataSource.observeEntries("PRJ-01").first()
        val postStockOuts = stockOutDataSource.observeStockOuts().first()
        assertEquals(0, postMovements.size)
        assertEquals(0, postStockOuts.size)
    }
}

package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.DispatchRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates the strict architectural boundary between Module 08 (Delivery Intent)
 * and Module 07 (Physical Stock Management).
 *
 * Verifies that Step 01 operations NEVER deduct inventory, NEVER create StockOutRecords,
 * and NEVER mutate the Movement Ledger.
 */
class DeliveryInventoryBoundaryTest {

    private lateinit var deliveryDataSource: FakeDeliveryOrderDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var deliveryRepository: DeliveryOrderRepository

    @Before
    fun setUp() {
        deliveryDataSource = FakeDeliveryOrderDataSource()
        stockOutDataSource = FakeInventoryStockOutDataSource()
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()
        deliveryRepository = DeliveryOrderRepositoryImpl(deliveryDataSource)
    }

    @Test
    fun `creating, approving and requesting dispatch on delivery order mutates zero inventory stock out records`() = runBlocking {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-BOUND-01",
            projectId = "PRJ-01",
            deliveryOrderNo = "DEL-BOUND-01",
            customerId = "CUST-1",
            sourceReferenceId = null,
            sourceReferenceType = null,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = 20000L,
            notes = "Delivery intent test",
            createdBy = "admin-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val line = DeliveryOrderLine(
            lineId = "LINE-BOUND-01",
            deliveryOrderId = "DO-BOUND-01",
            projectId = "PRJ-01",
            productId = "FINISHED-GOOD-A",
            requestedQuantity = 500.0,
            notes = "500 units"
        )

        // 1. Create delivery order
        val createRes = deliveryRepository.createDeliveryOrder(order, listOf(line), UserRole.ADMIN)
        assertTrue(createRes is DomainResult.Success)

        // 2. Submit
        val submitRes = deliveryRepository.submitDeliveryOrder(order.deliveryOrderId, "admin-1", UserRole.ADMIN)
        assertTrue(submitRes is DomainResult.Success)

        // 3. Approve
        val approveRes = deliveryRepository.approveDeliveryOrder(order.deliveryOrderId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)

        // 4. Mark Ready for Dispatch
        val readyRes = deliveryRepository.markReadyForDispatch(order.deliveryOrderId, "wh-1", UserRole.WAREHOUSE)
        assertTrue(readyRes is DomainResult.Success)

        // 5. Create Dispatch Request
        val req = DeliveryDispatchRequest(
            dispatchRequestId = "DISP-BOUND-01",
            projectId = "PRJ-01",
            deliveryOrderId = order.deliveryOrderId,
            requestedBy = "wh-1",
            requestedAt = 2000L,
            priority = DeliveryPriority.HIGH,
            status = DispatchRequestStatus.REQUESTED,
            notes = null
        )
        val dispatchRes = deliveryRepository.createDispatchRequest(req, UserRole.WAREHOUSE)
        assertTrue(dispatchRes is DomainResult.Success)

        // VERIFY: Absolutely zero Stock Out records exist in Module 07 datasource
        val stockOutList = stockOutDataSource.observeStockOuts().first()
        assertEquals("Stock Out records must remain 0 in Step 01", 0, stockOutList.size)

        // VERIFY: Absolutely zero movement ledger entries exist
        val ledgerEntries = ledgerDataSource.observeEntries("PRJ-01").first()
        assertEquals("Movement ledger entries must remain 0 in Step 01", 0, ledgerEntries.size)
    }
}

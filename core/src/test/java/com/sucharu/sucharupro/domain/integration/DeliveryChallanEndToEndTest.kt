package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.repository.DeliveryChallanRepositoryImpl
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
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
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryChallanEndToEndTest {

    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanRepository: DeliveryChallanRepository
    private lateinit var doRepository: DeliveryOrderRepository

    @Before
    fun setUp() {
        challanDataSource = FakeDeliveryChallanDataSource()
        doDataSource = FakeDeliveryOrderDataSource()
        challanRepository = DeliveryChallanRepositoryImpl(challanDataSource, doDataSource)
        doRepository = DeliveryOrderRepositoryImpl(doDataSource)
    }

    @Test
    fun `complete end-to-end delivery challan workflow with partial allocations`() = runBlocking {
        // Step 1: Create and Approve Delivery Order
        val deliveryOrder = DeliveryOrder(
            deliveryOrderId = "DO-E2E-1",
            projectId = "PRJ-E2E",
            deliveryOrderNo = "DEL-E2E-001",
            customerId = "CUST-E2E",
            sourceReferenceId = "SO-1001",
            sourceReferenceType = "SALES_ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = System.currentTimeMillis() + 86400000L,
            notes = "Urgent print delivery",
            createdBy = "sales-mgr",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val doLineA = DeliveryOrderLine(
            lineId = "DOLINE-A",
            deliveryOrderId = "DO-E2E-1",
            projectId = "PRJ-E2E",
            productId = "PROD-FLYER",
            requestedQuantity = 1000.0,
            notes = "Glossy paper"
        )
        val doLineB = DeliveryOrderLine(
            lineId = "DOLINE-B",
            deliveryOrderId = "DO-E2E-1",
            projectId = "PRJ-E2E",
            productId = "PROD-BANNER",
            requestedQuantity = 50.0,
            notes = "Outdoor vinyl"
        )

        doRepository.createDeliveryOrder(deliveryOrder, listOf(doLineA, doLineB), UserRole.ADMIN)
        doRepository.submitDeliveryOrder("DO-E2E-1", "sales-mgr", UserRole.ADMIN)
        doRepository.approveDeliveryOrder("DO-E2E-1", "general-mgr", UserRole.MANAGER)

        // Step 2: Create First Partial Challan (600 Flyers + 50 Banners)
        val challan1 = DeliveryChallan(
            challanId = "CH-01",
            projectId = "PRJ-E2E",
            challanNo = "CH-2026-001",
            deliveryOrderId = "DO-E2E-1",
            customerId = "CUST-E2E",
            sourceReferenceId = "SO-1001",
            sourceReferenceType = "SALES_ORDER",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = System.currentTimeMillis(),
            notes = "Batch 1 delivery",
            createdBy = "dispatch-mgr",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val ch1LineA = DeliveryChallanLine(
            lineId = "CH1-L1",
            challanId = "CH-01",
            projectId = "PRJ-E2E",
            deliveryOrderLineId = "DOLINE-A",
            productId = "PROD-FLYER",
            quantity = 600.0
        )
        val ch1LineB = DeliveryChallanLine(
            lineId = "CH1-L2",
            challanId = "CH-01",
            projectId = "PRJ-E2E",
            deliveryOrderLineId = "DOLINE-B",
            productId = "PROD-BANNER",
            quantity = 50.0
        )

        val createCh1Res = challanRepository.createChallan(challan1, listOf(ch1LineA, ch1LineB), UserRole.MANAGER)
        assertTrue(createCh1Res is DomainResult.Success)

        // Submit & Approve Challan 1
        challanRepository.submitChallan("CH-01", "dispatch-mgr", UserRole.MANAGER)
        challanRepository.approveChallan("CH-01", "general-mgr", UserRole.MANAGER)
        challanRepository.markReadyForDispatch("CH-01", "wh-mgr", UserRole.WAREHOUSE)

        // Step 3: Check remaining allocations
        val allocatedA = challanRepository.getAllocatedQuantityForDeliveryOrderLine("DOLINE-A")
        val allocatedB = challanRepository.getAllocatedQuantityForDeliveryOrderLine("DOLINE-B")
        assertEquals(600.0, allocatedA, 0.001)
        assertEquals(50.0, allocatedB, 0.001)

        // Step 4: Attempting to allocate more Banners should be rejected (all 50 allocated)
        val challan2 = DeliveryChallan(
            challanId = "CH-02",
            projectId = "PRJ-E2E",
            challanNo = "CH-2026-002",
            deliveryOrderId = "DO-E2E-1",
            customerId = "CUST-E2E",
            sourceReferenceId = "SO-1001",
            sourceReferenceType = "SALES_ORDER",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = System.currentTimeMillis(),
            notes = "Batch 2 delivery",
            createdBy = "dispatch-mgr",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val ch2LineBOver = DeliveryChallanLine(
            lineId = "CH2-L-INVALID",
            challanId = "CH-02",
            projectId = "PRJ-E2E",
            deliveryOrderLineId = "DOLINE-B",
            productId = "PROD-BANNER",
            quantity = 10.0 // Excess!
        )

        val overAllocRes = challanRepository.createChallan(challan2, listOf(ch2LineBOver), UserRole.MANAGER)
        assertTrue(overAllocRes is DomainResult.Error)

        // Step 5: Allocate remaining 400 Flyers on Challan 2
        val ch2LineAValid = DeliveryChallanLine(
            lineId = "CH2-L1",
            challanId = "CH-02",
            projectId = "PRJ-E2E",
            deliveryOrderLineId = "DOLINE-A",
            productId = "PROD-FLYER",
            quantity = 400.0
        )
        val createCh2Res = challanRepository.createChallan(challan2, listOf(ch2LineAValid), UserRole.MANAGER)
        assertTrue(createCh2Res is DomainResult.Success)

        challanRepository.submitChallan("CH-02", "dispatch-mgr", UserRole.MANAGER)
        challanRepository.approveChallan("CH-02", "general-mgr", UserRole.MANAGER)
        challanRepository.markReadyForDispatch("CH-02", "wh-mgr", UserRole.WAREHOUSE)

        // Step 6: Verify full allocation reached
        assertEquals(1000.0, challanRepository.getAllocatedQuantityForDeliveryOrderLine("DOLINE-A"), 0.001)
        assertEquals(50.0, challanRepository.getAllocatedQuantityForDeliveryOrderLine("DOLINE-B"), 0.001)

        // Step 7: Verify all Challans for DO-E2E-1
        val challansList = challanRepository.observeChallansForDeliveryOrder("DO-E2E-1").first()
        assertEquals(2, challansList.size)
        assertTrue(challansList.all { it.status == DeliveryChallanStatus.READY_FOR_DISPATCH })
    }
}

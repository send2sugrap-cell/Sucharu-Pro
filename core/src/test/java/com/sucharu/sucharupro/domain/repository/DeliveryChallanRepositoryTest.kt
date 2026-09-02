package com.sucharu.sucharupro.domain.repository

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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryChallanRepositoryTest {

    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanRepository: DeliveryChallanRepository
    private lateinit var doRepository: DeliveryOrderRepository

    @Before
    fun setUp() {
        runBlocking {
            challanDataSource = FakeDeliveryChallanDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            challanRepository = DeliveryChallanRepositoryImpl(challanDataSource, doDataSource)
            doRepository = DeliveryOrderRepositoryImpl(doDataSource)

            // Seed an approved Delivery Order
            val order = DeliveryOrder(
                deliveryOrderId = "DO-100",
                projectId = "PRJ-01",
                deliveryOrderNo = "DEL-100",
                customerId = "CUST-1",
                sourceReferenceId = null,
                sourceReferenceType = null,
                deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
                priority = DeliveryPriority.NORMAL,
                status = DeliveryOrderStatus.DRAFT,
                requestedDeliveryDate = 20000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val line = DeliveryOrderLine(
                lineId = "DOLINE-100",
                deliveryOrderId = "DO-100",
                projectId = "PRJ-01",
                productId = "PROD-1",
                requestedQuantity = 100.0,
                notes = null
            )

            doRepository.createDeliveryOrder(order, listOf(line), UserRole.ADMIN)
            doRepository.submitDeliveryOrder("DO-100", "user-1", UserRole.ADMIN)
            doRepository.approveDeliveryOrder("DO-100", "user-1", UserRole.ADMIN)
        }
    }

    private fun sampleChallan(
        challanId: String = "CHAL-1",
        challanNo: String = "CH-001"
    ): DeliveryChallan {
        return DeliveryChallan(
            challanId = challanId,
            projectId = "PRJ-01",
            challanNo = challanNo,
            deliveryOrderId = "DO-100",
            customerId = "CUST-1",
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = 15000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 15000L,
            updatedAt = 15000L
        )
    }

    private fun sampleLine(
        lineId: String = "CLINE-1",
        challanId: String = "CHAL-1",
        quantity: Double = 50.0
    ): DeliveryChallanLine {
        return DeliveryChallanLine(
            lineId = lineId,
            challanId = challanId,
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOLINE-100",
            productId = "PROD-1",
            quantity = quantity
        )
    }

    @Test
    fun `createChallan successfully creates challan and lines`() = runBlocking {
        val challan = sampleChallan()
        val lines = listOf(sampleLine())

        val result = challanRepository.createChallan(challan, lines, UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)

        val fetched = challanRepository.getChallan(challan.challanId, UserRole.ADMIN)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(challan.challanNo, (fetched as DomainResult.Success).data.challanNo)

        // Once approved, it consumes committed allocation
        challanRepository.submitChallan(challan.challanId, "user-1", UserRole.ADMIN)
        challanRepository.approveChallan(challan.challanId, "user-1", UserRole.ADMIN)
        val allocated = challanRepository.getAllocatedQuantityForDeliveryOrderLine("DOLINE-100")
        assertEquals(50.0, allocated, 0.001)
    }

    @Test
    fun `duplicate challanNo in same project is rejected`() = runBlocking {
        val ch1 = sampleChallan(challanId = "CH-A", challanNo = "CH-DUP")
        val lines1 = listOf(sampleLine(lineId = "L-A", challanId = "CH-A", quantity = 20.0))
        challanRepository.createChallan(ch1, lines1, UserRole.ADMIN)

        val ch2 = sampleChallan(challanId = "CH-B", challanNo = "CH-DUP")
        val lines2 = listOf(sampleLine(lineId = "L-B", challanId = "CH-B", quantity = 20.0))
        val result = challanRepository.createChallan(ch2, lines2, UserRole.ADMIN)

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun `cancelling a challan frees up active allocated quantity`() = runBlocking {
        val ch1 = sampleChallan(challanId = "CH-1", challanNo = "CH-001")
        val lines1 = listOf(sampleLine(lineId = "L-1", challanId = "CH-1", quantity = 80.0))
        challanRepository.createChallan(ch1, lines1, UserRole.ADMIN)
        challanRepository.submitChallan("CH-1", "user-1", UserRole.ADMIN)
        challanRepository.approveChallan("CH-1", "user-1", UserRole.ADMIN)

        // Attempting to create another 50.0 should fail (80 + 50 > 100)
        val ch2 = sampleChallan(challanId = "CH-2", challanNo = "CH-002")
        val lines2 = listOf(sampleLine(lineId = "L-2", challanId = "CH-2", quantity = 50.0))
        val failResult = challanRepository.createChallan(ch2, lines2, UserRole.ADMIN)
        assertTrue(failResult is DomainResult.Error)

        // Cancel CH-1
        challanRepository.cancelChallan("CH-1", "user-1", "Cancelled by user", UserRole.ADMIN)

        // Now creating CH-2 with 50.0 should succeed!
        val successResult = challanRepository.createChallan(ch2, lines2, UserRole.ADMIN)
        assertTrue(successResult is DomainResult.Success)
    }

    @Test
    fun `submit, approve, and mark ready for dispatch transitions correctly`() = runBlocking {
        val challan = sampleChallan()
        val lines = listOf(sampleLine())
        challanRepository.createChallan(challan, lines, UserRole.ADMIN)

        val submitRes = challanRepository.submitChallan(challan.challanId, "user-1", UserRole.ADMIN)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(DeliveryChallanStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        val approveRes = challanRepository.approveChallan(challan.challanId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(DeliveryChallanStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        val readyRes = challanRepository.markReadyForDispatch(challan.challanId, "wh-1", UserRole.WAREHOUSE)
        assertTrue(readyRes is DomainResult.Success)
        assertEquals(DeliveryChallanStatus.READY_FOR_DISPATCH, (readyRes as DomainResult.Success).data.status)
    }
}

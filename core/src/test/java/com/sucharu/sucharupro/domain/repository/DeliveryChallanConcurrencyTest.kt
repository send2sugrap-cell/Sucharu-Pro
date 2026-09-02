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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryChallanConcurrencyTest {

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

            val order = DeliveryOrder(
                deliveryOrderId = "DO-CONCUR-100",
                projectId = "PRJ-01",
                deliveryOrderNo = "DEL-CONCUR",
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
                lineId = "DOLINE-CONCUR",
                deliveryOrderId = "DO-CONCUR-100",
                projectId = "PRJ-01",
                productId = "PROD-1",
                requestedQuantity = 100.0,
                notes = null
            )
            doRepository.createDeliveryOrder(order, listOf(line), UserRole.ADMIN)
        }
    }

    @Test
    fun `concurrent approval of competing challans prevents over allocation`() = runBlocking {
        val ch1 = DeliveryChallan(
            challanId = "CH-C1",
            projectId = "PRJ-01",
            challanNo = "CH-001",
            deliveryOrderId = "DO-CONCUR-100",
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
        val line1 = DeliveryChallanLine(
            lineId = "CL-1",
            challanId = "CH-C1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOLINE-CONCUR",
            productId = "PROD-1",
            quantity = 60.0
        )

        val ch2 = DeliveryChallan(
            challanId = "CH-C2",
            projectId = "PRJ-01",
            challanNo = "CH-002",
            deliveryOrderId = "DO-CONCUR-100",
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
        val line2 = DeliveryChallanLine(
            lineId = "CL-2",
            challanId = "CH-C2",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOLINE-CONCUR",
            productId = "PROD-1",
            quantity = 60.0
        )

        challanRepository.createChallan(ch1, listOf(line1), UserRole.ADMIN)
        challanRepository.submitChallan("CH-C1", "user-1", UserRole.ADMIN)

        // CH-2 created when CH-1 was draft/pending
        // We will insert CH-2 and submit it
        challanDataSource.insertChallan(ch2, listOf(line2))
        challanDataSource.updateChallan(ch2.copy(status = DeliveryChallanStatus.PENDING))

        // Concurrently attempt to approve both
        val results = listOf("CH-C1", "CH-C2").map { id ->
            async(Dispatchers.IO) {
                challanRepository.approveChallan(id, "admin-user", UserRole.ADMIN)
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(1, errorCount)

        val allocated = challanRepository.getAllocatedQuantityForDeliveryOrderLine("DOLINE-CONCUR")
        assertEquals(60.0, allocated, 0.001)
    }
}

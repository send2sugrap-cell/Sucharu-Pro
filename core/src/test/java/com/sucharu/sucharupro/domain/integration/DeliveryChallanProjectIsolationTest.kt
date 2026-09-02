package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
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

class DeliveryChallanProjectIsolationTest {

    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanRepository: DeliveryChallanRepository

    @Before
    fun setUp() {
        runBlocking {
            challanDataSource = FakeDeliveryChallanDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            challanRepository = DeliveryChallanRepositoryImpl(challanDataSource, doDataSource)

            // Seed project A DO
            val doA = DeliveryOrder(
                deliveryOrderId = "DO-A",
                projectId = "PRJ-A",
                deliveryOrderNo = "DEL-A",
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
            val lineA = DeliveryOrderLine(
                lineId = "DOLINE-A",
                deliveryOrderId = "DO-A",
                projectId = "PRJ-A",
                productId = "PROD-A",
                requestedQuantity = 50.0,
                notes = null
            )
            doDataSource.insertDeliveryOrder(doA, listOf(lineA))

            // Seed project B DO
            val doB = DeliveryOrder(
                deliveryOrderId = "DO-B",
                projectId = "PRJ-B",
                deliveryOrderNo = "DEL-B",
                customerId = null,
                sourceReferenceId = null,
                sourceReferenceType = null,
                deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
                priority = DeliveryPriority.NORMAL,
                status = DeliveryOrderStatus.APPROVED,
                requestedDeliveryDate = 20000L,
                notes = null,
                createdBy = "user-2",
                createdAt = 1000L,
                updatedAt = 1000L
            )
            val lineB = DeliveryOrderLine(
                lineId = "DOLINE-B",
                deliveryOrderId = "DO-B",
                projectId = "PRJ-B",
                productId = "PROD-B",
                requestedQuantity = 50.0,
                notes = null
            )
            doDataSource.insertDeliveryOrder(doB, listOf(lineB))
        }
    }

    @Test
    fun `observeChallans returns strictly project scoped challans`() = runBlocking {
        val chA = DeliveryChallan(
            challanId = "CH-A",
            projectId = "PRJ-A",
            challanNo = "CH-001",
            deliveryOrderId = "DO-A",
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
        val lineA = DeliveryChallanLine("CLA", "CH-A", "PRJ-A", "DOLINE-A", "PROD-A", 10.0)
        challanRepository.createChallan(chA, listOf(lineA), UserRole.ADMIN, "PRJ-A")

        val chB = DeliveryChallan(
            challanId = "CH-B",
            projectId = "PRJ-B",
            challanNo = "CH-001", // same number, different project is allowed
            deliveryOrderId = "DO-B",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = 1000L,
            notes = null,
            createdBy = "user-2",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val lineB = DeliveryChallanLine("CLB", "CH-B", "PRJ-B", "DOLINE-B", "PROD-B", 20.0)
        challanRepository.createChallan(chB, listOf(lineB), UserRole.ADMIN, "PRJ-B")

        val listA = challanRepository.observeChallans("PRJ-A").first()
        val listB = challanRepository.observeChallans("PRJ-B").first()

        assertEquals(1, listA.size)
        assertEquals("CH-A", listA[0].challanId)

        assertEquals(1, listB.size)
        assertEquals("CH-B", listB[0].challanId)
    }

    @Test
    fun `cross project getChallan is blocked`() = runBlocking {
        val chA = DeliveryChallan(
            challanId = "CH-A",
            projectId = "PRJ-A",
            challanNo = "CH-001",
            deliveryOrderId = "DO-A",
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
        val lineA = DeliveryChallanLine("CLA", "CH-A", "PRJ-A", "DOLINE-A", "PROD-A", 10.0)
        challanRepository.createChallan(chA, listOf(lineA), UserRole.ADMIN, "PRJ-A")

        val result = challanRepository.getChallan("CH-A", UserRole.ADMIN, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Access denied"))
    }
}

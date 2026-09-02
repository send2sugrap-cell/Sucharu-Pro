package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryAnalyticsPODIntegrationTest {

    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var governanceDataSource: FakeDeliveryGovernanceDataSource
    private lateinit var repository: DeliveryAnalyticsRepository

    @Before
    fun setUp() {
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        proofDataSource = FakeDeliveryProofDataSource()
        governanceDataSource = FakeDeliveryGovernanceDataSource()
        repository = DeliveryAnalyticsRepositoryImpl(
            governanceDataSource = governanceDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource,
            shipmentDataSource = shipmentDataSource,
            proofDataSource = proofDataSource
        )
    }

    @Test
    fun `accepted proof of delivery correctly drives pod metrics and rate`() = runBlocking {
        val order = DeliveryOrder(
            deliveryOrderId = "DO-1",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-1",
            customerId = "CUST-1",
            sourceReferenceId = "SO-1",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DELIVERED,
            requestedDeliveryDate = 2000L,
            notes = null,
            createdBy = "u1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryOrderLine("DOL-1", "DO-1", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))

        shipmentDataSource.insertShipment(
            DeliveryShipment(
                shipmentId = "SH-1",
                projectId = "PRJ-01",
                deliveryOrderId = "DO-1",
                deliveryChallanId = "DC-1",
                dispatchExecutionId = "DE-1",
                shipmentNo = "SHN-1",
                carrierName = "Sundarban",
                trackingNumber = "TRK-1",
                estimatedDeliveryAt = 2000L,
                currentStatus = DeliveryShipmentStatus.DELIVERED,
                notes = null,
                createdBy = "u1",
                createdAt = 1000L,
                updatedAt = 1900L
            )
        )

        proofDataSource.insertProof(
            DeliveryProof(
                proofId = "POD-1",
                projectId = "PRJ-01",
                deliveryOrderId = "DO-1",
                deliveryChallanId = "DC-1",
                dispatchExecutionId = "DE-1",
                deliveryShipmentId = "SH-1",
                proofNo = "POD-NO-1",
                proofType = DeliveryProofType.SIGNATURE,
                recipientName = "Alice",
                proofStatus = DeliveryProofStatus.ACCEPTED,
                verifiedBy = "inspector-1",
                acceptedBy = "manager-1",
                notes = null,
                createdBy = "driver-1",
                createdAt = 1920L,
                updatedAt = 1980L
            )
        )

        val result = repository.getSummary(DeliveryAnalyticsFilter(projectId = "PRJ-01"), UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
        val summary = (result as DomainResult.Success).data
        assertEquals(1, summary.totalAcceptedPod)
        assertEquals(100.0, summary.podAcceptanceRate, 0.001)
    }
}

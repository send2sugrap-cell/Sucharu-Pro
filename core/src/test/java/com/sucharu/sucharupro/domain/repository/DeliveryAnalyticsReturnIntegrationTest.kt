package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.repository.DeliveryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnReason
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryAnalyticsReturnIntegrationTest {

    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var governanceDataSource: FakeDeliveryGovernanceDataSource
    private lateinit var repository: DeliveryAnalyticsRepository

    @Before
    fun setUp() {
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        returnDataSource = FakeDeliveryReturnDataSource()
        governanceDataSource = FakeDeliveryGovernanceDataSource()
        repository = DeliveryAnalyticsRepositoryImpl(
            governanceDataSource = governanceDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource,
            returnDataSource = returnDataSource
        )
    }

    @Test
    fun `returns correctly integrate and calculate return rates`() = runBlocking {
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

        val challan = DeliveryChallan(
            challanId = "DC-1",
            projectId = "PRJ-01",
            challanNo = "CN-1",
            deliveryOrderId = "DO-1",
            customerId = "CUST-1",
            sourceReferenceId = "SO-1",
            sourceReferenceType = "SO",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DELIVERED,
            issueDate = 1000L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val cLine = DeliveryChallanLine("CL-1", "DC-1", "PRJ-01", "DOL-1", "P-1", 100.0)
        challanDataSource.insertChallan(challan, listOf(cLine))

        val dispatch = DispatchExecution(
            dispatchExecutionId = "DE-1",
            projectId = "PRJ-01",
            dispatchNo = "DN-1",
            deliveryOrderId = "DO-1",
            deliveryChallanId = "DC-1",
            customerId = "CUST-1",
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DISPATCHED,
            stockOutId = "SOUT-1",
            dispatchDate = 1000L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L,
            dispatchedAt = 1000L,
            dispatchedBy = "wh-1"
        )
        val dLine = DispatchExecutionLine("DL-1", "PRJ-01", "DE-1", "CL-1", "DOL-1", "P-1", 100.0, 100.0, null, null, "LOC-01", 1000L)
        dispatchDataSource.insertDispatch(dispatch, listOf(dLine))

        val ret = DeliveryReturn(
            returnId = "RET-1",
            projectId = "PRJ-01",
            returnNo = "RET-NO-1",
            deliveryOrderId = "DO-1",
            deliveryChallanId = "DC-1",
            dispatchExecutionId = "DE-1",
            customerId = "CUST-1",
            returnReason = DeliveryReturnReason.DAMAGED,
            status = DeliveryReturnStatus.APPROVED,
            requestedBy = "u1",
            approvedBy = "manager-1",
            approvedAt = 1500L,
            createdAt = 1200L,
            updatedAt = 1500L
        )
        val rLine = DeliveryReturnLine(
            returnLineId = "RL-1",
            returnId = "RET-1",
            projectId = "PRJ-01",
            deliveryOrderLineId = "DOL-1",
            productId = "P-1",
            returnedQuantity = 100.0,
            acceptedQuantity = 0.0,
            rejectedQuantity = 0.0
        )
        returnDataSource.insertReturn(ret, listOf(rLine))

        val result = repository.getSummary(DeliveryAnalyticsFilter(projectId = "PRJ-01"), UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
        val summary = (result as DomainResult.Success).data
        assertEquals(1, summary.totalReturned)
    }
}

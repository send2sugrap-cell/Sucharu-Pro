package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.DeliveryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertCategory
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertSeverity
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeliveryAnalyticsInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var governanceDataSource: FakeDeliveryGovernanceDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var repository: DeliveryAnalyticsRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()
        governanceDataSource = FakeDeliveryGovernanceDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        repository = DeliveryAnalyticsRepositoryImpl(
            governanceDataSource = governanceDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource
        )
    }

    @Test
    fun `analytics operations and alert resolutions perform zero inventory mutations`() = runBlocking {
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

        governanceDataSource.insertAlert(
            DeliveryGovernanceAlert(
                alertId = "ALT-1",
                projectId = "PRJ-01",
                category = DeliveryGovernanceAlertCategory.OVERDUE_DELIVERY,
                severity = DeliveryGovernanceAlertSeverity.CRITICAL,
                referenceType = "SHIPMENT",
                referenceId = "SH-1",
                title = "Overdue",
                description = "Overdue",
                detectedAt = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val beforeStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val beforeStockIns = receivingDataSource.observeStockInRecords().first().size
        val beforeLedger = ledgerDataSource.getEntries("PRJ-01").size

        repository.getSummary(DeliveryAnalyticsFilter(projectId = "PRJ-01"), UserRole.ADMIN)
        repository.acknowledgeAlert("ALT-1", "admin", UserRole.ADMIN)
        repository.resolveAlert("ALT-1", "admin", "Resolved notes", UserRole.ADMIN)

        val afterStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val afterStockIns = receivingDataSource.observeStockInRecords().first().size
        val afterLedger = ledgerDataSource.getEntries("PRJ-01").size

        assertEquals(beforeStockOuts, afterStockOuts)
        assertEquals(beforeStockIns, afterStockIns)
        assertEquals(beforeLedger, afterLedger)
    }
}

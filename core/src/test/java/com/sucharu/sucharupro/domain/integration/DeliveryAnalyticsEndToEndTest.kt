package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.DeliveryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProof
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofType
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryAnalyticsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryAnalyticsEndToEndTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var returnDataSource: FakeDeliveryReturnDataSource
    private lateinit var proofDataSource: FakeDeliveryProofDataSource
    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var governanceDataSource: FakeDeliveryGovernanceDataSource
    private lateinit var repository: DeliveryAnalyticsRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        challanDataSource = FakeDeliveryChallanDataSource()
        dispatchDataSource = FakeDispatchExecutionDataSource()
        shipmentDataSource = FakeDeliveryShipmentDataSource()
        verificationDataSource = FakeDeliveryItemVerificationDataSource()
        returnDataSource = FakeDeliveryReturnDataSource()
        proofDataSource = FakeDeliveryProofDataSource()
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        governanceDataSource = FakeDeliveryGovernanceDataSource()

        repository = DeliveryAnalyticsRepositoryImpl(
            governanceDataSource = governanceDataSource,
            orderDataSource = orderDataSource,
            challanDataSource = challanDataSource,
            dispatchDataSource = dispatchDataSource,
            shipmentDataSource = shipmentDataSource,
            verificationDataSource = verificationDataSource,
            returnDataSource = returnDataSource,
            proofDataSource = proofDataSource,
            reconciliationDataSource = reconciliationDataSource
        )
    }

    @Test
    fun `full end to end delivery analytics and governance workflow`() = runBlocking {
        val projectId = "PRJ-E2E-10"

        // 1. Delivery Order & Lines
        val order = DeliveryOrder(
            deliveryOrderId = "DO-E2E-10",
            projectId = projectId,
            deliveryOrderNo = "DON-E2E-10",
            customerId = "CUST-E2E-1",
            sourceReferenceId = "SO-E2E-1",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.DELIVERED,
            requestedDeliveryDate = 2000L,
            notes = null,
            createdBy = "admin-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryOrderLine("DOL-E2E-1", "DO-E2E-10", projectId, "P-1", 1000.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))

        // 2. Delivery Challan
        val challan = DeliveryChallan(
            challanId = "DC-E2E-10",
            projectId = projectId,
            challanNo = "DCN-E2E-10",
            deliveryOrderId = "DO-E2E-10",
            customerId = "CUST-E2E-1",
            sourceReferenceId = "SO-E2E-1",
            sourceReferenceType = "SO",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DELIVERED,
            issueDate = 1100L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1100L,
            updatedAt = 1100L
        )
        val cLine = DeliveryChallanLine("DCL-E2E-1", "DC-E2E-10", projectId, "DOL-E2E-1", "P-1", 1000.0)
        challanDataSource.insertChallan(challan, listOf(cLine))

        // 3. Dispatch Execution
        val dispatch = DispatchExecution(
            dispatchExecutionId = "DE-E2E-10",
            projectId = projectId,
            dispatchNo = "DEN-E2E-10",
            deliveryOrderId = "DO-E2E-10",
            deliveryChallanId = "DC-E2E-10",
            customerId = "CUST-E2E-1",
            sourceWarehouseId = "WH-Main",
            sourceLocationId = "WH-A1",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DISPATCHED,
            stockOutId = "SOUT-E2E-10",
            dispatchDate = 1200L,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1200L,
            updatedAt = 1200L,
            dispatchedAt = 1200L,
            dispatchedBy = "wh-1"
        )
        val dLine = DispatchExecutionLine("DEL-E2E-1", projectId, "DE-E2E-10", "DCL-E2E-1", "DOL-E2E-1", "P-1", 1000.0, 1000.0, null, null, "WH-A1", 1200L)
        dispatchDataSource.insertDispatch(dispatch, listOf(dLine))

        // 4. Delivery Shipment
        val shipment = DeliveryShipment(
            shipmentId = "SH-E2E-10",
            projectId = projectId,
            shipmentNo = "SHN-E2E-10",
            deliveryOrderId = "DO-E2E-10",
            deliveryChallanId = "DC-E2E-10",
            dispatchExecutionId = "DE-E2E-10",
            carrierName = "Sundarban Express",
            trackingNumber = "TRK-E2E-10",
            estimatedDeliveryAt = 2000L,
            currentStatus = DeliveryShipmentStatus.DELIVERED,
            notes = null,
            createdBy = "wh-1",
            createdAt = 1250L,
            updatedAt = 1800L
        )
        shipmentDataSource.insertShipment(shipment)

        // 5. Proof of Delivery (Accepted)
        val proof = DeliveryProof(
            proofId = "POD-E2E-10",
            projectId = projectId,
            deliveryOrderId = "DO-E2E-10",
            deliveryChallanId = "DC-E2E-10",
            dispatchExecutionId = "DE-E2E-10",
            deliveryShipmentId = "SH-E2E-10",
            customerId = "CUST-E2E-1",
            proofNo = "POD-NO-E2E-10",
            proofType = DeliveryProofType.SIGNATURE,
            recipientName = "Customer Officer",
            proofStatus = DeliveryProofStatus.ACCEPTED,
            verifiedBy = "qc-1",
            acceptedBy = "manager-1",
            notes = null,
            createdBy = "driver-1",
            createdAt = 1810L,
            updatedAt = 1900L
        )
        proofDataSource.insertProof(proof)

        // 6. Delivery Reconciliation
        val reconciliation = DeliveryReconciliation(
            reconciliationId = "REC-E2E-10",
            projectId = projectId,
            deliveryOrderId = "DO-E2E-10",
            orderedQuantity = 1000.0,
            dispatchedQuantity = 1000.0,
            deliveredQuantity = 950.0,
            acceptedPodQuantity = 950.0,
            returnedQuantity = 50.0,
            outstandingQuantity = 0.0,
            discrepancyQuantity = 0.0,
            reconciliationStatus = DeliveryReconciliationStatus.RECONCILED,
            createdBy = "admin-1",
            createdAt = 1950L,
            updatedAt = 1950L
        )
        val recItem = DeliveryReconciliationItem(
            reconciliationItemId = "RECI-E2E-1",
            reconciliationId = "REC-E2E-10",
            projectId = projectId,
            deliveryOrderLineId = "DOL-E2E-1",
            productId = "P-1",
            orderedQuantity = 1000.0,
            dispatchedQuantity = 1000.0,
            deliveredQuantity = 950.0,
            acceptedPodQuantity = 950.0,
            returnedQuantity = 50.0,
            outstandingQuantity = 0.0,
            discrepancyQuantity = 0.0
        )
        reconciliationDataSource.insertReconciliation(reconciliation, listOf(recItem))

        // Step 10 Query: Summary & Rates
        val summaryResult = repository.getSummary(
            DeliveryAnalyticsFilter(projectId = projectId),
            UserRole.ADMIN
        )
        assertTrue(summaryResult is DomainResult.Success)
        val summary = (summaryResult as DomainResult.Success).data
        assertEquals(1, summary.totalDeliveryOrders)
        assertEquals(1, summary.totalShipments)
        assertEquals(1, summary.totalAcceptedPod)
        assertEquals(95.0, summary.deliverySuccessRate, 0.001)
        assertEquals(100.0, summary.podAcceptanceRate, 0.001)

        // Step 10 Query: Trends
        val trendsResult = repository.getTrends(projectId, DeliveryAnalyticsPeriod.ALL_TIME, UserRole.ADMIN)
        assertTrue(trendsResult is DomainResult.Success)

        // Step 10 Query: Breakdown
        val breakdownResult = repository.getBreakdown(DeliveryAnalyticsFilter(projectId = projectId), UserRole.ADMIN)
        assertTrue(breakdownResult is DomainResult.Success)

        // Step 10 Governance: Scan & Generate Alerts
        val alertsResult = repository.refreshGovernanceAlerts(projectId, "admin-1", UserRole.ADMIN)
        assertTrue(alertsResult is DomainResult.Success)

        // Step 10 Inventory Boundary: Verify zero ledger mutations
        assertEquals(0, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(0, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(0, ledgerDataSource.getEntries(projectId).size)
    }
}

package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockTransferDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryTraceabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DeliveryChallanRepositoryImpl
import com.sucharu.sucharupro.data.repository.DeliveryItemVerificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.DeliveryOrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.DispatchExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryTraceabilityRepositoryImpl
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
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import com.sucharu.sucharupro.domain.repository.DeliveryItemVerificationRepository
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeliveryItemVerificationEndToEndTest {

    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var traceabilityDataSource: FakeInventoryTraceabilityDataSource

    private lateinit var doRepository: DeliveryOrderRepository
    private lateinit var challanRepository: DeliveryChallanRepository
    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var traceabilityRepository: InventoryTraceabilityRepository
    private lateinit var dispatchRepository: DispatchExecutionRepository
    private lateinit var verificationRepository: DeliveryItemVerificationRepository

    @Before
    fun setUp() {
        runBlocking {
            doDataSource = FakeDeliveryOrderDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            verificationDataSource = FakeDeliveryItemVerificationDataSource()
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            traceabilityDataSource = FakeInventoryTraceabilityDataSource()

            doRepository = DeliveryOrderRepositoryImpl(doDataSource)
            challanRepository = DeliveryChallanRepositoryImpl(challanDataSource, doDataSource)

            stockOutRepository = InventoryStockOutRepositoryImpl(
                stockOutDataSource, receivingDataSource, productDataSource, warehouseDataSource, locationDataSource
            )

            traceabilityRepository = InventoryTraceabilityRepositoryImpl(
                traceabilityDataSource, productDataSource, receivingDataSource, stockOutDataSource,
                FakeInventoryStockTransferDataSource(), FakeInventoryStockAdjustmentDataSource()
            )

            dispatchRepository = DispatchExecutionRepositoryImpl(
                dispatchDataSource, challanDataSource, stockOutRepository, traceabilityRepository
            )

            verificationRepository = DeliveryItemVerificationRepositoryImpl(
                verificationDataSource, dispatchDataSource, challanDataSource, doDataSource
            )

            // Seed inventory infrastructure
            warehouseDataSource.insertWarehouse(
                InventoryWarehouse(
                    id = "WH-01",
                    projectId = "PRJ-E2E",
                    code = "WH-1",
                    name = "Warehouse 1",
                    type = InventoryWarehouseType.FINISHED_GOODS,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            locationDataSource.insertLocation(
                InventoryLocation(
                    id = "LOC-01",
                    projectId = "PRJ-E2E",
                    warehouseId = "WH-01",
                    code = "LOC-1",
                    name = "Bay 1",
                    type = InventoryLocationType.SHELF,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            productDataSource.insertProduct(
                InventoryProduct(
                    id = "PROD-E2E",
                    sku = "SKU-1",
                    name = "Magazine Print",
                    isActive = true,
                    isStockTracked = true,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(
                    stockInId = UUID.randomUUID().toString(),
                    receivingId = "REC-1",
                    receivingLineId = "RL-1",
                    projectId = "PRJ-E2E",
                    inventoryProductId = "PROD-E2E",
                    warehouseId = "WH-01",
                    locationId = "LOC-01",
                    quantity = 1000,
                    unit = InventoryUnit.PCS,
                    createdBy = "admin",
                    createdAt = "1000"
                )
            )
        }
    }

    @Test
    fun `complete end-to-end delivery verification pipeline`() = runBlocking {
        // Step 1: Delivery Order
        val doOrder = DeliveryOrder(
            deliveryOrderId = "DO-E2E-4",
            projectId = "PRJ-E2E",
            deliveryOrderNo = "DEL-E2E-4",
            customerId = "CUST-1",
            sourceReferenceId = "SO-1",
            sourceReferenceType = "SALES_ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = 50000L,
            notes = null,
            createdBy = "admin",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val doLine = DeliveryOrderLine(
            lineId = "DOL-E2E-4",
            deliveryOrderId = "DO-E2E-4",
            projectId = "PRJ-E2E",
            productId = "PROD-E2E",
            requestedQuantity = 300.0,
            notes = null
        )
        doRepository.createDeliveryOrder(doOrder, listOf(doLine), UserRole.ADMIN)
        doRepository.submitDeliveryOrder("DO-E2E-4", "admin", UserRole.ADMIN)
        doRepository.approveDeliveryOrder("DO-E2E-4", "mgr", UserRole.MANAGER)

        // Step 2: Delivery Challan
        val challan = DeliveryChallan(
            challanId = "CH-E2E-4",
            projectId = "PRJ-E2E",
            challanNo = "CHAL-E2E-4",
            deliveryOrderId = "DO-E2E-4",
            customerId = "CUST-1",
            sourceReferenceId = "SO-1",
            sourceReferenceType = "SALES_ORDER",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = 2000L,
            notes = null,
            createdBy = "admin",
            createdAt = 2000L,
            updatedAt = 2000L
        )
        val challanLine = DeliveryChallanLine(
            lineId = "CL-E2E-4",
            challanId = "CH-E2E-4",
            projectId = "PRJ-E2E",
            deliveryOrderLineId = "DOL-E2E-4",
            productId = "PROD-E2E",
            quantity = 300.0
        )
        challanRepository.createChallan(challan, listOf(challanLine), UserRole.MANAGER)
        challanRepository.submitChallan("CH-E2E-4", "admin", UserRole.MANAGER)
        challanRepository.approveChallan("CH-E2E-4", "mgr", UserRole.MANAGER)
        challanRepository.markReadyForDispatch("CH-E2E-4", "wh", UserRole.WAREHOUSE)

        // Step 3: Dispatch Execution
        val dispatch = DispatchExecution(
            dispatchExecutionId = "DISP-E2E-4",
            projectId = "PRJ-E2E",
            dispatchNo = "DN-E2E-4",
            deliveryOrderId = "DO-E2E-4",
            deliveryChallanId = "CH-E2E-4",
            customerId = "CUST-1",
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DRAFT,
            dispatchDate = 3000L,
            notes = null,
            createdBy = "admin",
            createdAt = 3000L,
            updatedAt = 3000L
        )
        val dispatchLine = DispatchExecutionLine(
            dispatchExecutionLineId = "DL-E2E-4",
            projectId = "PRJ-E2E",
            dispatchExecutionId = "DISP-E2E-4",
            deliveryChallanLineId = "CL-E2E-4",
            deliveryOrderLineId = "DOL-E2E-4",
            productId = "PROD-E2E",
            requestedQuantity = 300.0,
            dispatchQuantity = 300.0,
            batchId = null,
            lotId = null,
            sourceLocationId = "LOC-01",
            createdAt = 3000L
        )
        dispatchRepository.createDispatch(dispatch, listOf(dispatchLine), UserRole.MANAGER)
        dispatchRepository.submitDispatch("DISP-E2E-4", "admin", UserRole.MANAGER)
        dispatchRepository.approveDispatch("DISP-E2E-4", "mgr", UserRole.MANAGER)
        dispatchRepository.markReadyForExecution("DISP-E2E-4", "wh", UserRole.WAREHOUSE)
        dispatchRepository.executeDispatch("DISP-E2E-4", "operator", UserRole.WAREHOUSE)

        // Step 4: Create Delivery Item Verification
        val verification = DeliveryItemVerification(
            verificationId = "V-E2E-4",
            projectId = "PRJ-E2E",
            verificationNo = "VERIF-E2E-4",
            deliveryOrderId = "DO-E2E-4",
            deliveryChallanId = "CH-E2E-4",
            dispatchExecutionId = "DISP-E2E-4",
            status = DeliveryItemVerificationStatus.DRAFT,
            remarks = null,
            createdBy = "operator",
            createdAt = 4000L,
            updatedAt = 4000L
        )
        val vLine = DeliveryItemVerificationLine(
            verificationLineId = "VL-E2E-4",
            verificationId = "V-E2E-4",
            projectId = "PRJ-E2E",
            dispatchExecutionLineId = "DL-E2E-4",
            challanLineId = "CL-E2E-4",
            deliveryOrderLineId = "DOL-E2E-4",
            productId = "PROD-E2E",
            batchId = null,
            lotId = null,
            expectedQuantity = 300.0,
            verifiedQuantity = 300.0,
            issueQuantity = 0.0,
            createdAt = 4000L
        )
        val createVerifRes = verificationRepository.createVerification(verification, listOf(vLine), UserRole.WAREHOUSE)
        assertTrue(createVerifRes is DomainResult.Success)

        // Step 5: Submit and Start Verification
        verificationRepository.submitVerification("V-E2E-4", "operator", UserRole.WAREHOUSE)
        verificationRepository.startVerification("V-E2E-4", "operator", UserRole.WAREHOUSE)

        // Step 6: Verify Line item
        val lineVerifRes = verificationRepository.verifyLine(
            verificationId = "V-E2E-4",
            verificationLineId = "VL-E2E-4",
            verifiedQuantity = 300.0,
            isDamaged = false,
            damagedQuantity = 0.0,
            isMissing = false,
            isProductMismatch = false,
            isBatchMismatch = false,
            isLotMismatch = false,
            remarks = "All 300 magazines in pristine condition",
            actorId = "operator",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(lineVerifRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationResultType.VERIFIED, (lineVerifRes as DomainResult.Success).data.resultType)

        // Step 7: Complete and Close Verification
        val completeRes = verificationRepository.completeVerification("V-E2E-4", "operator", UserRole.WAREHOUSE)
        assertTrue(completeRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationStatus.VERIFIED, (completeRes as DomainResult.Success).data.status)

        val closeRes = verificationRepository.closeVerification("V-E2E-4", "mgr", UserRole.MANAGER)
        assertTrue(closeRes is DomainResult.Success)
        assertEquals(DeliveryItemVerificationStatus.CLOSED, (closeRes as DomainResult.Success).data.status)

        // Step 8: Verify Reconciliation Summary
        val summaryRes = verificationRepository.getVerificationSummary("V-E2E-4", UserRole.ADMIN)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(300.0, summary.expectedTotalQuantity, 0.001)
        assertEquals(300.0, summary.verifiedTotalQuantity, 0.001)
        assertEquals(false, summary.hasDiscrepancies)

        // Step 9: Verify Audit Trail
        val auditRes = verificationRepository.getActivityEvents("V-E2E-4", UserRole.ADMIN)
        assertTrue(auditRes is DomainResult.Success)
        val audit = (auditRes as DomainResult.Success).data
        assertTrue(audit.any { it.activityType == DeliveryItemVerificationActivityType.CREATED })
        assertTrue(audit.any { it.activityType == DeliveryItemVerificationActivityType.STARTED })
        assertTrue(audit.any { it.activityType == DeliveryItemVerificationActivityType.LINE_VERIFIED })
        assertTrue(audit.any { it.activityType == DeliveryItemVerificationActivityType.VERIFIED })
        assertTrue(audit.any { it.activityType == DeliveryItemVerificationActivityType.CLOSED })
    }
}

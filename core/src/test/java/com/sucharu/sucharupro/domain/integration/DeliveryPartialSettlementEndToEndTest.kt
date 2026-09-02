package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryShipmentDataSource
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
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.data.repository.DeliveryShipmentRepositoryImpl
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
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEventType
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
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
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeliveryPartialSettlementEndToEndTest {

    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
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
    private lateinit var shipmentRepository: DeliveryShipmentRepository
    private lateinit var settlementRepository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            doDataSource = FakeDeliveryOrderDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            verificationDataSource = FakeDeliveryItemVerificationDataSource()
            shipmentDataSource = FakeDeliveryShipmentDataSource()
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
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

            shipmentRepository = DeliveryShipmentRepositoryImpl(
                shipmentDataSource, dispatchDataSource, verificationDataSource, challanDataSource, doDataSource
            )

            settlementRepository = DeliveryPartialSettlementRepositoryImpl(
                settlementDataSource = settlementDataSource,
                doDataSource = doDataSource,
                challanDataSource = challanDataSource,
                dispatchDataSource = dispatchDataSource,
                verificationDataSource = verificationDataSource,
                shipmentDataSource = shipmentDataSource
            )

            // Seed inventory: 2000 pcs in Finished Goods
            warehouseDataSource.insertWarehouse(
                InventoryWarehouse("WH-01", "PRJ-E2E-6", "WH-1", "Warehouse 1", null, InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin", createdAt = "1000", updatedAt = "1000")
            )
            locationDataSource.insertLocation(
                InventoryLocation(
                    id = "LOC-01",
                    projectId = "PRJ-E2E-6",
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
                    id = "PROD-E2E-6",
                    sku = "SKU-BOOK",
                    name = "Hardcover Book Print",
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(UUID.randomUUID().toString(), "REC-1", "RL-1", "PRJ-E2E-6", "PROD-E2E-6", "WH-01", "LOC-01", 2000, InventoryUnit.PCS, "admin", "1000")
            )
        }
    }

    @Test
    fun `complete end-to-end multi-split delivery and final settlement lifecycle`() = runBlocking {
        // Step 1: Delivery Order for 1,000 units
        val doOrder = DeliveryOrder("DO-E2E-6", "PRJ-E2E-6", "DEL-E2E-6", "CUST-1", "SO-1", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.HIGH, DeliveryOrderStatus.DRAFT, 2000L, null, "admin", 1000L, 1000L)
        val doLine = DeliveryOrderLine("DOL-E2E-6", "DO-E2E-6", "PRJ-E2E-6", "PROD-E2E-6", 1000.0, null)
        doRepository.createDeliveryOrder(doOrder, listOf(doLine), UserRole.ADMIN)
        doRepository.submitDeliveryOrder("DO-E2E-6", "admin", UserRole.ADMIN)
        doRepository.approveDeliveryOrder("DO-E2E-6", "mgr", UserRole.MANAGER)

        // Step 2: Initialize Settlement
        val initRes = settlementRepository.initializeSettlementForDeliveryOrder("DO-E2E-6", "admin", UserRole.ADMIN)
        assertTrue(initRes is DomainResult.Success)
        val settlement = (initRes as DomainResult.Success).data
        val sId = settlement.settlementId
        assertEquals(1000.0, settlement.totalOrderedQuantity, 0.001)
        assertEquals(1000.0, settlement.totalPendingQuantity, 0.001)
        assertEquals(DeliverySettlementStatus.OPEN, settlement.status)

        // Step 3: First Split Delivery (Batch #1: 600 units)
        // Challan 1: 600 units
        val ch1 = DeliveryChallan("CH-E2E-6-1", "PRJ-E2E-6", "CHAL-1", "DO-E2E-6", "CUST-1", "SO-1", "SALES_ORDER", DeliveryChallanType.STANDARD, DeliveryChallanStatus.DRAFT, 2000L, null, "admin", 2000L, 2000L)
        val cl1 = DeliveryChallanLine("CL-1", "CH-E2E-6-1", "PRJ-E2E-6", "DOL-E2E-6", "PROD-E2E-6", 600.0)
        challanRepository.createChallan(ch1, listOf(cl1), UserRole.MANAGER)
        challanRepository.submitChallan("CH-E2E-6-1", "admin", UserRole.MANAGER)
        challanRepository.approveChallan("CH-E2E-6-1", "mgr", UserRole.MANAGER)
        challanRepository.markReadyForDispatch("CH-E2E-6-1", "wh", UserRole.WAREHOUSE)

        // Dispatch 1: 600 units
        val disp1 = DispatchExecution("DISP-E2E-6-1", "PRJ-E2E-6", "DN-1", "DO-E2E-6", "CH-E2E-6-1", "CUST-1", "WH-01", "LOC-01", DispatchExecutionType.STANDARD, DispatchExecutionStatus.DRAFT, null, 3000L, null, "admin", 3000L, 3000L)
        val dl1 = DispatchExecutionLine("DL-1", "PRJ-E2E-6", "DISP-E2E-6-1", "CL-1", "DOL-E2E-6", "PROD-E2E-6", 600.0, 600.0, null, null, "LOC-01", 3000L)
        dispatchRepository.createDispatch(disp1, listOf(dl1), UserRole.MANAGER)
        dispatchRepository.submitDispatch("DISP-E2E-6-1", "admin", UserRole.MANAGER)
        dispatchRepository.approveDispatch("DISP-E2E-6-1", "mgr", UserRole.MANAGER)
        dispatchRepository.markReadyForExecution("DISP-E2E-6-1", "wh", UserRole.WAREHOUSE)
        dispatchRepository.executeDispatch("DISP-E2E-6-1", "operator", UserRole.WAREHOUSE)

        // Verification 1: 600 units verified
        val ver1 = DeliveryItemVerification("V-1", "PRJ-E2E-6", "VN-1", "DO-E2E-6", "CH-E2E-6-1", "DISP-E2E-6-1", DeliveryItemVerificationStatus.DRAFT, null, null, null, "operator", 4000L, null, 4000L)
        val vl1 = DeliveryItemVerificationLine("VL-1", "V-1", "PRJ-E2E-6", "DL-1", "CL-1", "DOL-E2E-6", "PROD-E2E-6", null, null, 600.0, 600.0, 0.0, createdAt = 4000L)
        verificationRepository.createVerification(ver1, listOf(vl1), UserRole.WAREHOUSE)
        verificationRepository.submitVerification("V-1", "operator", UserRole.WAREHOUSE)
        verificationRepository.startVerification("V-1", "operator", UserRole.WAREHOUSE)
        verificationRepository.verifyLine("V-1", "VL-1", 600.0, false, 0.0, false, false, false, false, "Batch 1 verified", "operator", UserRole.WAREHOUSE)
        verificationRepository.completeVerification("V-1", "operator", UserRole.WAREHOUSE)
        verificationRepository.closeVerification("V-1", "mgr", UserRole.MANAGER)

        // Shipment 1: 600 units delivered
        val shp1 = DeliveryShipment("SHP-1", "PRJ-E2E-6", "SN-1", "DO-E2E-6", "CH-E2E-6-1", "DISP-E2E-6-1", verificationId = "V-1", currentStatus = DeliveryShipmentStatus.DELIVERED, createdBy = "operator", createdAt = 5000L, updatedAt = 5000L)
        shipmentRepository.createShipment(shp1, UserRole.WAREHOUSE)

        // Record Split #1 in Settlement
        val splitLine1 = DeliverySplitDispatchLine("SDL-1", "PRJ-E2E-6", "", "DOL-E2E-6", "PROD-E2E-6", 600.0, createdAt = 5000L)
        settlementRepository.createSplitDispatch("DO-E2E-6", listOf(splitLine1), "CH-E2E-6-1", "DISP-E2E-6-1", "SHP-1", "First partial batch", "operator", UserRole.WAREHOUSE)

        // Recalculate settlement after First Split
        val recalc1 = settlementRepository.recalculateSettlement(sId, "operator", UserRole.WAREHOUSE)
        assertTrue(recalc1 is DomainResult.Success)
        val sAfterBatch1 = (recalc1 as DomainResult.Success).data
        assertEquals(600.0, sAfterBatch1.totalDeliveredQuantity, 0.001)
        assertEquals(400.0, sAfterBatch1.totalPendingQuantity, 0.001)
        assertEquals(DeliverySettlementStatus.PARTIALLY_DELIVERED, sAfterBatch1.status)

        // Step 4: Second Split Delivery (Batch #2: remaining 400 units)
        // Challan 2: 400 units
        val ch2 = DeliveryChallan("CH-E2E-6-2", "PRJ-E2E-6", "CHAL-2", "DO-E2E-6", "CUST-1", "SO-1", "SALES_ORDER", DeliveryChallanType.STANDARD, DeliveryChallanStatus.DRAFT, 6000L, null, "admin", 6000L, 6000L)
        val cl2 = DeliveryChallanLine("CL-2", "CH-E2E-6-2", "PRJ-E2E-6", "DOL-E2E-6", "PROD-E2E-6", 400.0)
        challanRepository.createChallan(ch2, listOf(cl2), UserRole.MANAGER)
        challanRepository.submitChallan("CH-E2E-6-2", "admin", UserRole.MANAGER)
        challanRepository.approveChallan("CH-E2E-6-2", "mgr", UserRole.MANAGER)
        challanRepository.markReadyForDispatch("CH-E2E-6-2", "wh", UserRole.WAREHOUSE)

        // Dispatch 2: 400 units
        val disp2 = DispatchExecution("DISP-E2E-6-2", "PRJ-E2E-6", "DN-2", "DO-E2E-6", "CH-E2E-6-2", "CUST-1", "WH-01", "LOC-01", DispatchExecutionType.STANDARD, DispatchExecutionStatus.DRAFT, null, 7000L, null, "admin", 7000L, 7000L)
        val dl2 = DispatchExecutionLine("DL-2", "PRJ-E2E-6", "DISP-E2E-6-2", "CL-2", "DOL-E2E-6", "PROD-E2E-6", 400.0, 400.0, null, null, "LOC-01", 7000L)
        dispatchRepository.createDispatch(disp2, listOf(dl2), UserRole.MANAGER)
        dispatchRepository.submitDispatch("DISP-E2E-6-2", "admin", UserRole.MANAGER)
        dispatchRepository.approveDispatch("DISP-E2E-6-2", "mgr", UserRole.MANAGER)
        dispatchRepository.markReadyForExecution("DISP-E2E-6-2", "wh", UserRole.WAREHOUSE)
        dispatchRepository.executeDispatch("DISP-E2E-6-2", "operator", UserRole.WAREHOUSE)

        // Verification 2: 400 units verified
        val ver2 = DeliveryItemVerification("V-2", "PRJ-E2E-6", "VN-2", "DO-E2E-6", "CH-E2E-6-2", "DISP-E2E-6-2", DeliveryItemVerificationStatus.DRAFT, null, null, null, "operator", 8000L, null, 8000L)
        val vl2 = DeliveryItemVerificationLine("VL-2", "V-2", "PRJ-E2E-6", "DL-2", "CL-2", "DOL-E2E-6", "PROD-E2E-6", null, null, 400.0, 400.0, 0.0, createdAt = 8000L)
        verificationRepository.createVerification(ver2, listOf(vl2), UserRole.WAREHOUSE)
        verificationRepository.submitVerification("V-2", "operator", UserRole.WAREHOUSE)
        verificationRepository.startVerification("V-2", "operator", UserRole.WAREHOUSE)
        verificationRepository.verifyLine("V-2", "VL-2", 400.0, false, 0.0, false, false, false, false, "Batch 2 verified", "operator", UserRole.WAREHOUSE)
        verificationRepository.completeVerification("V-2", "operator", UserRole.WAREHOUSE)
        verificationRepository.closeVerification("V-2", "mgr", UserRole.MANAGER)

        // Shipment 2: 400 units delivered
        val shp2 = DeliveryShipment("SHP-2", "PRJ-E2E-6", "SN-2", "DO-E2E-6", "CH-E2E-6-2", "DISP-E2E-6-2", verificationId = "V-2", currentStatus = DeliveryShipmentStatus.DELIVERED, createdBy = "operator", createdAt = 9000L, updatedAt = 9000L)
        shipmentRepository.createShipment(shp2, UserRole.WAREHOUSE)

        // Record Split #2 in Settlement
        val splitLine2 = DeliverySplitDispatchLine("SDL-2", "PRJ-E2E-6", "", "DOL-E2E-6", "PROD-E2E-6", 400.0, createdAt = 9000L)
        settlementRepository.createSplitDispatch("DO-E2E-6", listOf(splitLine2), "CH-E2E-6-2", "DISP-E2E-6-2", "SHP-2", "Second and final batch", "operator", UserRole.WAREHOUSE)

        // Recalculate settlement after Second Split
        val recalc2 = settlementRepository.recalculateSettlement(sId, "operator", UserRole.WAREHOUSE)
        assertTrue(recalc2 is DomainResult.Success)
        val sAfterBatch2 = (recalc2 as DomainResult.Success).data
        assertEquals(1000.0, sAfterBatch2.totalDeliveredQuantity, 0.001)
        assertEquals(0.0, sAfterBatch2.totalPendingQuantity, 0.001)
        assertEquals(DeliverySettlementStatus.FULLY_DELIVERED, sAfterBatch2.status)

        // Step 5: Finalize Settlement
        val finalizeRes = settlementRepository.finalizeSettlement(sId, "Full 1,000 units verified and received across 2 splits", "mgr", UserRole.MANAGER)
        assertTrue(finalizeRes is DomainResult.Success)
        val finalSettlement = (finalizeRes as DomainResult.Success).data
        assertEquals(DeliverySettlementStatus.SETTLED, finalSettlement.status)
        assertTrue(finalSettlement.isFullySettled)

        // Verify Audit Trail & Splits History
        val splits = (settlementRepository.getSplitDispatches("DO-E2E-6", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(2, splits.size)
        assertEquals(1, splits[0].splitSequence)
        assertEquals(2, splits[1].splitSequence)

        val events = (settlementRepository.getEvents(sId, UserRole.ADMIN) as DomainResult.Success).data
        assertTrue(events.any { it.eventType == DeliverySettlementEventType.CREATED })
        assertTrue(events.any { it.eventType == DeliverySettlementEventType.SPLIT_CREATED })
        assertTrue(events.any { it.eventType == DeliverySettlementEventType.SETTLED })
    }
}

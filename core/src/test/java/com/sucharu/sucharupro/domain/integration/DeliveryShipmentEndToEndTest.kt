package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
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
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttemptStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEventType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentType
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

class DeliveryShipmentEndToEndTest {

    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var shipmentDataSource: FakeDeliveryShipmentDataSource
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

    @Before
    fun setUp() {
        runBlocking {
            doDataSource = FakeDeliveryOrderDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            verificationDataSource = FakeDeliveryItemVerificationDataSource()
            shipmentDataSource = FakeDeliveryShipmentDataSource()
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

            // Seed inventory
            warehouseDataSource.insertWarehouse(
                InventoryWarehouse("WH-01", "PRJ-E2E-5", "WH-1", "Warehouse 1", null, InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin", createdAt = "1000", updatedAt = "1000")
            )
            locationDataSource.insertLocation(
                InventoryLocation(
                    id = "LOC-01",
                    projectId = "PRJ-E2E-5",
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
                    id = "PROD-E2E-5",
                    sku = "SKU-1",
                    name = "Brochure Print",
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            receivingDataSource.insertStockInRecord(
                InventoryStockInRecord(UUID.randomUUID().toString(), "REC-1", "RL-1", "PRJ-E2E-5", "PROD-E2E-5", "WH-01", "LOC-01", 1000, InventoryUnit.PCS, "admin", "1000")
            )
        }
    }

    @Test
    fun `complete end-to-end delivery shipment and tracking lifecycle`() = runBlocking {
        // Step 1: Delivery Order
        val doOrder = DeliveryOrder("DO-E2E-5", "PRJ-E2E-5", "DEL-E2E-5", "CUST-1", "SO-1", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.HIGH, DeliveryOrderStatus.DRAFT, 50000L, null, "admin", 1000L, 1000L)
        val doLine = DeliveryOrderLine("DOL-E2E-5", "DO-E2E-5", "PRJ-E2E-5", "PROD-E2E-5", 200.0, null)
        doRepository.createDeliveryOrder(doOrder, listOf(doLine), UserRole.ADMIN)
        doRepository.submitDeliveryOrder("DO-E2E-5", "admin", UserRole.ADMIN)
        doRepository.approveDeliveryOrder("DO-E2E-5", "mgr", UserRole.MANAGER)

        // Step 2: Delivery Challan
        val challan = DeliveryChallan("CH-E2E-5", "PRJ-E2E-5", "CHAL-E2E-5", "DO-E2E-5", "CUST-1", "SO-1", "SALES_ORDER", DeliveryChallanType.STANDARD, DeliveryChallanStatus.DRAFT, 2000L, null, "admin", 2000L, 2000L)
        val challanLine = DeliveryChallanLine("CL-E2E-5", "CH-E2E-5", "PRJ-E2E-5", "DOL-E2E-5", "PROD-E2E-5", 200.0)
        challanRepository.createChallan(challan, listOf(challanLine), UserRole.MANAGER)
        challanRepository.submitChallan("CH-E2E-5", "admin", UserRole.MANAGER)
        challanRepository.approveChallan("CH-E2E-5", "mgr", UserRole.MANAGER)
        challanRepository.markReadyForDispatch("CH-E2E-5", "wh", UserRole.WAREHOUSE)

        // Step 3: Dispatch Execution
        val dispatch = DispatchExecution("DISP-E2E-5", "PRJ-E2E-5", "DN-E2E-5", "DO-E2E-5", "CH-E2E-5", "CUST-1", "WH-01", "LOC-01", DispatchExecutionType.STANDARD, DispatchExecutionStatus.DRAFT, null, 3000L, null, "admin", 3000L, 3000L)
        val dispatchLine = DispatchExecutionLine("DL-E2E-5", "PRJ-E2E-5", "DISP-E2E-5", "CL-E2E-5", "DOL-E2E-5", "PROD-E2E-5", 200.0, 200.0, null, null, "LOC-01", 3000L)
        dispatchRepository.createDispatch(dispatch, listOf(dispatchLine), UserRole.MANAGER)
        dispatchRepository.submitDispatch("DISP-E2E-5", "admin", UserRole.MANAGER)
        dispatchRepository.approveDispatch("DISP-E2E-5", "mgr", UserRole.MANAGER)
        dispatchRepository.markReadyForExecution("DISP-E2E-5", "wh", UserRole.WAREHOUSE)
        dispatchRepository.executeDispatch("DISP-E2E-5", "operator", UserRole.WAREHOUSE)

        // Step 4: Delivery Item Verification
        val verification = DeliveryItemVerification("V-E2E-5", "PRJ-E2E-5", "VERIF-E2E-5", "DO-E2E-5", "CH-E2E-5", "DISP-E2E-5", DeliveryItemVerificationStatus.DRAFT, null, null, null, "operator", 4000L, null, 4000L)
        val vLine = DeliveryItemVerificationLine("VL-E2E-5", "V-E2E-5", "PRJ-E2E-5", "DL-E2E-5", "CL-E2E-5", "DOL-E2E-5", "PROD-E2E-5", null, null, 200.0, 200.0, 0.0, createdAt = 4000L)
        verificationRepository.createVerification(verification, listOf(vLine), UserRole.WAREHOUSE)
        verificationRepository.submitVerification("V-E2E-5", "operator", UserRole.WAREHOUSE)
        verificationRepository.startVerification("V-E2E-5", "operator", UserRole.WAREHOUSE)
        verificationRepository.verifyLine("V-E2E-5", "VL-E2E-5", 200.0, false, 0.0, false, false, false, false, "All items verified", "operator", UserRole.WAREHOUSE)
        verificationRepository.completeVerification("V-E2E-5", "operator", UserRole.WAREHOUSE)
        verificationRepository.closeVerification("V-E2E-5", "mgr", UserRole.MANAGER)

        // Step 5: Create Delivery Shipment (Step 05)
        val shipment = DeliveryShipment(
            shipmentId = "SHP-E2E-5",
            projectId = "PRJ-E2E-5",
            shipmentNo = "SHIP-2026-005",
            deliveryOrderId = "DO-E2E-5",
            deliveryChallanId = "CH-E2E-5",
            dispatchExecutionId = "DISP-E2E-5",
            verificationId = "V-E2E-5",
            customerId = "CUST-1",
            shipmentType = DeliveryShipmentType.COURIER,
            priority = DeliveryShipmentPriority.HIGH,
            carrierName = "Steadfast Courier",
            carrierReference = "SF-BOOKING-998",
            trackingNumber = "SF-TRACK-12345",
            destinationAddress = "Plot 12, Gulshan-2, Dhaka",
            destinationContactName = "Mr. Karim",
            destinationContactPhone = "+8801711000000",
            currentStatus = DeliveryShipmentStatus.DRAFT,
            createdBy = "operator",
            createdAt = 5000L,
            updatedAt = 5000L
        )
        val createShipRes = shipmentRepository.createShipment(shipment, UserRole.WAREHOUSE)
        assertTrue(createShipRes is DomainResult.Success)

        // Step 6: Mark Ready -> Mark Dispatched -> In Transit -> Out For Delivery
        shipmentRepository.markReady("SHP-E2E-5", "operator", UserRole.WAREHOUSE)
        shipmentRepository.markDispatched("SHP-E2E-5", 6000L, "operator", UserRole.WAREHOUSE)
        shipmentRepository.markInTransit("SHP-E2E-5", "Central Sorting Hub", "In courier vehicle", "operator", UserRole.WAREHOUSE)
        shipmentRepository.markOutForDelivery("SHP-E2E-5", "Gulshan Hub Van 3", null, "operator", UserRole.WAREHOUSE)

        // Step 7: Record Delivery Attempt #1 (Recipient Unavailable)
        val attempt1Res = shipmentRepository.recordDeliveryAttempt(
            shipmentId = "SHP-E2E-5",
            status = DeliveryShipmentAttemptStatus.RECIPIENT_UNAVAILABLE,
            reason = "Recipient in meeting, gate requested afternoon delivery",
            notes = null,
            attemptedAt = 7000L,
            actorId = "courier",
            callerRole = UserRole.WAREHOUSE
        )
        assertTrue(attempt1Res is DomainResult.Success)

        // Step 8: Redeliver and Mark Delivered
        val deliveredRes = shipmentRepository.markDelivered("SHP-E2E-5", 8000L, "Delivered to recipient in person", "courier", UserRole.WAREHOUSE)
        assertTrue(deliveredRes is DomainResult.Success)

        // Step 9: Validate Final State & Timeline
        val finalShipment = (shipmentRepository.getShipment("SHP-E2E-5", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(DeliveryShipmentStatus.DELIVERED, finalShipment.currentStatus)
        assertEquals(8000L, finalShipment.actualDeliveryAt)

        val trackingEvents = (shipmentRepository.getTrackingEvents("SHP-E2E-5", UserRole.ADMIN) as DomainResult.Success).data
        assertTrue(trackingEvents.size >= 6)
        assertTrue(trackingEvents.any { it.eventType == DeliveryShipmentEventType.CREATED })
        assertTrue(trackingEvents.any { it.eventType == DeliveryShipmentEventType.READY })
        assertTrue(trackingEvents.any { it.eventType == DeliveryShipmentEventType.DISPATCHED })
        assertTrue(trackingEvents.any { it.eventType == DeliveryShipmentEventType.IN_TRANSIT })
        assertTrue(trackingEvents.any { it.eventType == DeliveryShipmentEventType.OUT_FOR_DELIVERY })
        assertTrue(trackingEvents.any { it.eventType == DeliveryShipmentEventType.DELIVERED })

        val attempts = (shipmentRepository.getDeliveryAttempts("SHP-E2E-5", UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(1, attempts.size)
        assertEquals(1, attempts[0].attemptNo)

        val activities = (shipmentRepository.getActivityEvents("SHP-E2E-5", UserRole.ADMIN) as DomainResult.Success).data
        assertTrue(activities.any { it.activityType == DeliveryShipmentActivityType.CREATED })
        assertTrue(activities.any { it.activityType == DeliveryShipmentActivityType.DELIVERED })
    }
}

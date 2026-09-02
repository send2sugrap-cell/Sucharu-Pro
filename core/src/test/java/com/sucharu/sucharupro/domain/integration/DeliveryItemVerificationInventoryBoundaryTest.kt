package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DeliveryItemVerificationRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
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
import com.sucharu.sucharupro.domain.repository.DeliveryItemVerificationRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeliveryItemVerificationInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var stockOutRepository: InventoryStockOutRepository
    private lateinit var verificationRepository: DeliveryItemVerificationRepository

    @Before
    fun setUp() {
        runBlocking {
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            dispatchDataSource = FakeDispatchExecutionDataSource()
            verificationDataSource = FakeDeliveryItemVerificationDataSource()

            stockOutRepository = InventoryStockOutRepositoryImpl(
                stockOutDataSource, receivingDataSource, productDataSource, warehouseDataSource, locationDataSource
            )

            verificationRepository = DeliveryItemVerificationRepositoryImpl(
                verificationDataSource = verificationDataSource,
                dispatchDataSource = dispatchDataSource
            )

            // Seed inventory
            warehouseDataSource.insertWarehouse(
                InventoryWarehouse(
                    id = "WH-01",
                    projectId = "PRJ-01",
                    code = "WH-1",
                    name = "Main WH",
                    type = InventoryWarehouseType.FINISHED_GOODS,
                    createdBy = "admin",
                    createdAt = "1000",
                    updatedAt = "1000"
                )
            )
            locationDataSource.insertLocation(
                InventoryLocation(
                    id = "LOC-01",
                    projectId = "PRJ-01",
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
                    id = "PROD-01",
                    sku = "SKU-1",
                    name = "Flyer",
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
                    projectId = "PRJ-01",
                    inventoryProductId = "PROD-01",
                    warehouseId = "WH-01",
                    locationId = "LOC-01",
                    quantity = 500,
                    unit = InventoryUnit.PCS,
                    createdBy = "admin",
                    createdAt = "1000"
                )
            )

            // Dispatched 100 units
            val dispatch = DispatchExecution(
                dispatchExecutionId = "DISP-BOUND",
                projectId = "PRJ-01",
                dispatchNo = "DN-BOUND",
                deliveryOrderId = "DO-01",
                deliveryChallanId = "CH-01",
                customerId = null,
                sourceWarehouseId = "WH-01",
                sourceLocationId = "LOC-01",
                dispatchType = DispatchExecutionType.STANDARD,
                status = DispatchExecutionStatus.DISPATCHED,
                stockOutId = "SO-BOUND",
                dispatchDate = 1000L,
                notes = null,
                createdBy = "user-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                dispatchedBy = "operator",
                dispatchedAt = 1000L
            )
            val dLine = DispatchExecutionLine(
                dispatchExecutionLineId = "DL-BOUND",
                projectId = "PRJ-01",
                dispatchExecutionId = "DISP-BOUND",
                deliveryChallanLineId = "CL-1",
                deliveryOrderLineId = "DOL-1",
                productId = "PROD-01",
                requestedQuantity = 100.0,
                dispatchQuantity = 100.0,
                batchId = null,
                lotId = null,
                sourceLocationId = "LOC-01",
                createdAt = 1000L
            )
            dispatchDataSource.insertDispatch(dispatch, listOf(dLine))
        }
    }

    @Test
    fun `delivery item verification creates zero inventory stock movements and preserves warehouse balances`() = runBlocking {
        val initialAvailableStock = stockOutRepository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-01")
        assertEquals(500, initialAvailableStock)

        val verification = DeliveryItemVerification(
            verificationId = "V-BOUND",
            projectId = "PRJ-01",
            verificationNo = "VN-BOUND",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            dispatchExecutionId = "DISP-BOUND",
            status = DeliveryItemVerificationStatus.DRAFT,
            remarks = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryItemVerificationLine(
            verificationLineId = "VL-BOUND",
            verificationId = "V-BOUND",
            projectId = "PRJ-01",
            dispatchExecutionLineId = "DL-BOUND",
            challanLineId = "CL-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-01",
            batchId = null,
            lotId = null,
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0,
            issueQuantity = 0.0,
            createdAt = 1000L
        )

        // 1. Create
        val createRes = verificationRepository.createVerification(verification, listOf(line), UserRole.ADMIN)
        assertTrue(createRes is DomainResult.Success)

        // 2. Submit & Start
        verificationRepository.submitVerification("V-BOUND", "user-1", UserRole.ADMIN)
        verificationRepository.startVerification("V-BOUND", "operator", UserRole.WAREHOUSE)

        // 3. Verify Line (even with discrepancy reported)
        verificationRepository.verifyLine(
            verificationId = "V-BOUND",
            verificationLineId = "VL-BOUND",
            verifiedQuantity = 80.0,
            isDamaged = true,
            damagedQuantity = 10.0,
            isMissing = false,
            isProductMismatch = false,
            isBatchMismatch = false,
            isLotMismatch = false,
            remarks = "10 damaged, 10 missing",
            actorId = "operator",
            callerRole = UserRole.WAREHOUSE
        )

        // 4. Complete & Close
        verificationRepository.completeVerification("V-BOUND", "operator", UserRole.WAREHOUSE)
        verificationRepository.closeVerification("V-BOUND", "mgr", UserRole.MANAGER)

        // PROVE: Zero StockOut records created by verification
        val stockOutRecords = stockOutDataSource.observeStockOuts().first()
        assertEquals(0, stockOutRecords.size)

        // PROVE: Warehouse stock balance remains exactly 500
        val finalAvailableStock = stockOutRepository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-01")
        assertEquals(500, finalAvailableStock)
    }
}

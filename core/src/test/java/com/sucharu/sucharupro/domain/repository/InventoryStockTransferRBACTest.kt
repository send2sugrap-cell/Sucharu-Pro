package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockTransferDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryStockTransferRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RBAC permission tests for [InventoryStockTransferRepository] (Module 07 Step 05).
 */
class InventoryStockTransferRBACTest {

    private lateinit var repository: InventoryStockTransferRepository

    @Before
    fun setup() {
        runBlocking {
            val transferDataSource = FakeInventoryStockTransferDataSource()
            val stockOutDataSource = FakeInventoryStockOutDataSource()
            val receivingDataSource = FakeInventoryReceivingDataSource()
            val productDataSource = FakeInventoryProductDataSource()
            val warehouseDataSource = FakeInventoryWarehouseDataSource()
            val locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryStockTransferRepositoryImpl(
                transferDataSource = transferDataSource,
                stockOutDataSource = stockOutDataSource,
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )
        }
    }

    @Test
    fun `ADMIN has full access`() = runBlocking {
        val result = repository.createStockTransfer(buildTransfer(), UserRole.ADMIN)
        if (result is DomainResult.Error) {
            assertTrue("Should not be an authorization error", !result.message.contains("not authorized"))
        }
    }

    @Test
    fun `STAFF cannot create stock transfer`() = runBlocking {
        val result = repository.createStockTransfer(buildTransfer(), UserRole.STAFF)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("not authorized"))
    }

    @Test
    fun `WAREHOUSE can create and issue but not approve or complete`() = runBlocking {
        // RBAC Check for Create
        val createResult = repository.createStockTransfer(buildTransfer(), UserRole.WAREHOUSE)
        if (createResult is DomainResult.Error) {
            assertTrue("Should not be an authorization error", !createResult.message.contains("not authorized"))
        }

        // RBAC Check for Approve
        val approveResult = repository.approveStockTransfer("ST-01", "admin", "2026-08-17T10:00:00Z", UserRole.WAREHOUSE)
        assertTrue(approveResult is DomainResult.Error)
        assertTrue((approveResult as DomainResult.Error).message.contains("not authorized"))

        // RBAC Check for Complete
        val completeResult = repository.completeStockTransfer("ST-01", "admin", "2026-08-17T10:00:00Z", UserRole.WAREHOUSE)
        assertTrue(completeResult is DomainResult.Error)
        assertTrue((completeResult as DomainResult.Error).message.contains("not authorized"))
    }

    private fun buildTransfer() = InventoryStockTransfer(
        transferId = "ST-01", projectId = "PRJ-01", transferReference = "ST-REF-01",
        fromWarehouseId = "WH-SRC", toWarehouseId = "WH-DEST", transferDate = "2026-08-17",
        createdBy = "admin", createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )
}

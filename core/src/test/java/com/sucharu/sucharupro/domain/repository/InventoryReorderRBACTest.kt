package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryReorderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryReorderRBACTest {

    private lateinit var repository: InventoryReorderRepository
    private val projectId = "PROJ-7"
    private val policy = InventoryStockLevelPolicy(
        policyId = "POL-1",
        projectId = projectId,
        productId = "PROD-1",
        reorderPoint = 10.0,
        targetStockLevel = 20.0,
        maximumStockLevel = 30.0
    )

    @Before
    fun setup() {
        repository = InventoryReorderRepositoryImpl(
            FakeInventoryReorderDataSource(),
            FakeInventoryReceivingDataSource(),
            FakeInventoryStockOutDataSource(),
            FakeInventoryStockTransferDataSource(),
            FakeInventoryStockAdjustmentDataSource(),
            FakeInventoryProductDataSource(),
            FakeInventoryWarehouseDataSource(),
            FakeInventoryLocationDataSource()
        )
    }

    @Test
    fun `ADMIN can create policy`() = runBlocking {
        val result = repository.createPolicy(policy, UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `MANAGER can create policy`() = runBlocking {
        val result = repository.createPolicy(policy, UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `WAREHOUSE cannot create policy`() = runBlocking {
        val result = repository.createPolicy(policy, UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("not authorized"))
    }

    @Test
    fun `STAFF cannot create policy`() = runBlocking {
        val result = repository.createPolicy(policy, UserRole.STAFF)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `WAREHOUSE can view policy`() = runBlocking {
        repository.createPolicy(policy, UserRole.ADMIN)
        val result = repository.getPolicy("POL-1", UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `STAFF cannot view policy`() = runBlocking {
        repository.createPolicy(policy, UserRole.ADMIN)
        val result = repository.getPolicy("POL-1", UserRole.STAFF)
        assertTrue(result is DomainResult.Error)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryWarehouseDeactivationTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `deactivating an active warehouse removes it from active stream but retains in all stream`() = runBlocking {
        val wh = InventoryWarehouse(
            id = "WH-DEACT-01",
            projectId = "PRJ-01",
            code = "WH-ACTIVE",
            name = "Active Hub",
            status = InventoryWarehouseStatus.ACTIVE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        repository.createWarehouse(wh, callerRole = UserRole.ADMIN)

        val deactRes = repository.deactivateWarehouse("WH-DEACT-01", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        assertTrue(deactRes is DomainResult.Success)
        val updated = (deactRes as DomainResult.Success).data
        assertEquals(InventoryWarehouseStatus.INACTIVE, updated.status)

        val activeList = repository.observeActiveWarehouses("PRJ-01").first()
        assertEquals(0, activeList.size)

        val allList = repository.observeWarehouses("PRJ-01").first()
        assertEquals(1, allList.size)
    }
}

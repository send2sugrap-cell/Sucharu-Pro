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

class InventoryWarehouseActivationTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `activating an inactive warehouse updates status and stream emission`() = runBlocking {
        val wh = InventoryWarehouse(
            id = "WH-ACT-01",
            projectId = "PRJ-01",
            code = "WH-INACTIVE",
            name = "Inactive Hub",
            status = InventoryWarehouseStatus.INACTIVE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        repository.createWarehouse(wh, callerRole = UserRole.ADMIN)

        val activeList1 = repository.observeActiveWarehouses("PRJ-01").first()
        assertEquals(0, activeList1.size)

        val activateRes = repository.activateWarehouse("WH-ACT-01", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        assertTrue(activateRes is DomainResult.Success)
        val updated = (activateRes as DomainResult.Success).data
        assertEquals(InventoryWarehouseStatus.ACTIVE, updated.status)

        val activeList2 = repository.observeActiveWarehouses("PRJ-01").first()
        assertEquals(1, activeList2.size)
        assertEquals("WH-ACT-01", activeList2.first().id)
    }
}

package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationEndToEndTest {

    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource

    private lateinit var warehouseRepository: InventoryWarehouseRepositoryImpl
    private lateinit var locationRepository: InventoryLocationRepositoryImpl

    @Before
    fun setup() {
        warehouseDataSource = FakeInventoryWarehouseDataSource()
        locationDataSource = FakeInventoryLocationDataSource()

        warehouseRepository = InventoryWarehouseRepositoryImpl(warehouseDataSource)
        locationRepository = InventoryLocationRepositoryImpl(locationDataSource, warehouseDataSource)
    }

    @Test
    fun `end-to-end warehouse and nested location hierarchy lifecycle with full verification`() = runBlocking {
        val projectId = "PRJ-PRINT-01"

        // 1. Create Main Warehouse
        val warehouse = InventoryWarehouse(
            id = "WH-MAIN-01",
            projectId = projectId,
            code = "WH-MAIN",
            name = "Main Quran & Book Facility",
            description = "Central printing finished goods storage",
            type = InventoryWarehouseType.FINISHED_GOODS,
            status = InventoryWarehouseStatus.ACTIVE,
            address = "Dhaka Central Printing Hub",
            contactPerson = "Hasan Mahmud",
            contactPhone = "+8801711223344",
            notes = "Primary facility",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val whRes = warehouseRepository.createWarehouse(warehouse, callerRole = UserRole.ADMIN)
        assertTrue(whRes is DomainResult.Success)

        // 2. Create Top-level Storage Zone in Warehouse
        val zone = InventoryLocation(
            id = "LOC-ZONE-A",
            projectId = projectId,
            warehouseId = "WH-MAIN-01",
            parentLocationId = null,
            code = "ZONE-QURAN",
            name = "Quran Sharif Zone",
            type = InventoryLocationType.ZONE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:30:00Z",
            updatedAt = "2026-08-17T08:30:00Z"
        )
        val zoneRes = locationRepository.createLocation(zone, callerRole = UserRole.ADMIN)
        assertTrue(zoneRes is DomainResult.Success)

        // 3. Create Child Rack in Zone
        val rack = InventoryLocation(
            id = "LOC-RACK-01",
            projectId = projectId,
            warehouseId = "WH-MAIN-01",
            parentLocationId = "LOC-ZONE-A",
            code = "RACK-01",
            name = "Quran 30 Para Rack 1",
            type = InventoryLocationType.RACK,
            capacity = 5000.0,
            capacityUnit = "PCS",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:45:00Z",
            updatedAt = "2026-08-17T08:45:00Z"
        )
        val rackRes = locationRepository.createLocation(rack, callerRole = UserRole.ADMIN)
        assertTrue(rackRes is DomainResult.Success)

        // 4. Create Leaf Bin on Rack
        val bin = InventoryLocation(
            id = "LOC-BIN-101",
            projectId = projectId,
            warehouseId = "WH-MAIN-01",
            parentLocationId = "LOC-RACK-01",
            code = "BIN-101",
            name = "Hafezi Quran Bin 101",
            type = InventoryLocationType.BIN,
            capacity = 500.0,
            capacityUnit = "PCS",
            createdBy = "admin-01",
            createdAt = "2026-08-17T09:00:00Z",
            updatedAt = "2026-08-17T09:00:00Z"
        )
        val binRes = locationRepository.createLocation(bin, callerRole = UserRole.ADMIN)
        assertTrue(binRes is DomainResult.Success)

        // 5. Query Child Locations of Rack
        val rackChildren = locationRepository.observeChildLocations("LOC-RACK-01", projectId).first()
        assertEquals(1, rackChildren.size)
        assertEquals("LOC-BIN-101", rackChildren.first().id)

        // 6. Query all Locations in Warehouse
        val whLocations = locationRepository.observeLocationsByWarehouse("WH-MAIN-01", projectId).first()
        assertEquals(3, whLocations.size)

        // 7. Verify Audit Trails
        val whEvents = warehouseRepository.observeActivityEvents(projectId).first()
        assertEquals(1, whEvents.size)

        val locEvents = locationRepository.observeActivityEvents(projectId).first()
        assertEquals(3, locEvents.size)

        // 8. Lifecycle Transitions
        val deactBin = locationRepository.deactivateLocation("LOC-BIN-101", "mgr-01", "2026-08-17T10:00:00Z", UserRole.MANAGER)
        assertTrue(deactBin is DomainResult.Success)
        assertEquals(InventoryLocationStatus.INACTIVE, (deactBin as DomainResult.Success).data.status)

        val activeInWh = locationRepository.observeActiveLocations("WH-MAIN-01", projectId).first()
        assertEquals(2, activeInWh.size) // Zone and Rack are active, Bin is inactive
    }
}

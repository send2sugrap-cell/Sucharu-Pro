package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateReservationStep02ServiceTest {

    private lateinit var service: SubstrateReservationService
    private lateinit var repository: SubstrateReservationRepositoryImpl
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        val fakeDs = FakeSubstrateReservationDataSource()
        repository = SubstrateReservationRepositoryImpl(fakeDs)
        service = SubstrateReservationServiceImpl(repository)
    }

    @Test
    fun `test complete Step 02 lifecycle - soft hold, promote to hard, multi-source allocation, and AI export`() = runBlocking {
        val req = SubstrateRequirement(
            requirementId = "REQ-101",
            tenantId = tenantId,
            orderId = "ORD-301",
            orderItemId = "ITEM-01",
            stockType = PaperStockType.ART_CARD,
            requestedMaterialCode = "ART-300-25X36",
            requestedMaterialName = "Art Card 300 GSM",
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            productiveSheetsRequired = 4500L,
            wasteSheetsRequired = 500L,
            totalSheetsRequired = 5000L,
            totalReamsRequired = BigDecimal("10.0000"),
            totalWeightKg = BigDecimal("50.0000")
        )

        // 1. Create Soft Reservation
        val softRes = service.createSoftReservation(
            tenantId = tenantId,
            orderId = "ORD-301",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            warehouseId = "WH-MAIN-01",
            locationId = null,
            requirement = req,
            softHoldDurationMinutes = 120L,
            notes = "Quotation hold",
            actor = "sales_rep_1"
        )

        assertNotNull(softRes)
        assertEquals(SubstrateReservationMode.SOFT, softRes.mode)
        assertEquals(SubstrateReservationStatus.RESERVED_SOFT, softRes.status)
        assertNotNull(softRes.softHoldExpiresAt)

        // 2. Promote Soft Hold to HARD
        val hardRes = service.promoteSoftToHard(
            tenantId = tenantId,
            reservationId = softRes.reservationId,
            executionJobId = "JOB-EXEC-901",
            workOrderId = "WO-901",
            allocatedWarehouseId = "WH-MAIN-01",
            allocatedLocationId = "BAY-C1",
            allocatedBatchNumber = "LOT-2026-X",
            actor = "production_manager"
        )

        assertNotNull(hardRes)
        assertEquals(SubstrateReservationMode.HARD, hardRes.mode)
        assertEquals(SubstrateReservationStatus.ALLOCATED_HARD, hardRes.status)
        assertEquals("JOB-EXEC-901", hardRes.executionJobId)
        assertEquals("production_manager", hardRes.promotedBy)
        assertNotNull(hardRes.promotedAt)
        assertEquals(1, hardRes.allocationSources.size)
        assertEquals("LOT-2026-X", hardRes.allocationSources[0].batchNumber)

        // 3. Multi-Source Allocation Update (Split across 2 warehouses)
        val multiSources = listOf(
            SubstrateAllocationSource(
                allocationId = "ALOC-1",
                reservationId = hardRes.reservationId,
                tenantId = tenantId,
                warehouseId = "WH-MAIN-01",
                locationId = "BAY-C1",
                batchNumber = "LOT-2026-X",
                allocatedSheets = 3000L,
                allocatedReams = BigDecimal("6.0000"),
                allocatedWeightKg = BigDecimal("30.0000"),
                allocatedBy = "production_manager"
            ),
            SubstrateAllocationSource(
                allocationId = "ALOC-2",
                reservationId = hardRes.reservationId,
                tenantId = tenantId,
                warehouseId = "WH-SECONDARY-02",
                locationId = "RACK-D4",
                batchNumber = "LOT-2026-Y",
                allocatedSheets = 2000L,
                allocatedReams = BigDecimal("4.0000"),
                allocatedWeightKg = BigDecimal("20.0000"),
                allocatedBy = "production_manager"
            )
        )

        val updatedAllocRes = service.allocateReservationSources(
            tenantId = tenantId,
            reservationId = hardRes.reservationId,
            sources = multiSources,
            actor = "production_manager"
        )

        assertEquals(2, updatedAllocRes.allocationSources.size)
        assertEquals(5000L, updatedAllocRes.allocationSources.sumOf { it.allocatedSheets })

        // 4. Export Step 02 Handoff Contract
        val step02Contract = service.exportStep02HandoffContract(tenantId, hardRes.reservationId)
        assertNotNull(step02Contract)
        assertEquals("2.0.0", step02Contract.contractVersion)
        assertEquals("HARD", step02Contract.mode)
        assertEquals("ALLOCATED_HARD", step02Contract.status)
        assertTrue(step02Contract.isHardAllocated)
        assertEquals(2, step02Contract.allocationSourcesCount)
    }
}

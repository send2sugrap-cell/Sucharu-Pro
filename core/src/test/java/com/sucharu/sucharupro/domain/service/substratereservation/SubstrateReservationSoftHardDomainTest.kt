package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SubstrateReservationSoftHardDomainTest {

    @Test
    fun `test soft reservation creation preserves SOFT mode and computes soft hold timeout`() {
        val now = System.currentTimeMillis()
        val durationMins = 120L
        val expiry = now + (durationMins * 60 * 1000L)

        val res = SubstrateReservation(
            reservationId = "SRES-001",
            tenantId = "TENANT-001",
            orderId = "ORD-001",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            warehouseId = "WH-MAIN-01",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            reservedSheets = 2500L,
            reservedReams = BigDecimal("5.0000"),
            reservedWeightKg = BigDecimal("25.0000"),
            status = SubstrateReservationStatus.RESERVED_SOFT,
            mode = SubstrateReservationMode.SOFT,
            idempotencyKey = "key-001",
            softHoldExpiresAt = expiry,
            reservedBy = "sales_agent"
        )

        assertEquals(SubstrateReservationMode.SOFT, res.mode)
        assertEquals(SubstrateReservationStatus.RESERVED_SOFT, res.status)
        assertTrue(res.status.isActiveHold)
        assertFalse(res.status.isTerminal)
        assertEquals(2500L, res.reservedSheets)
        assertEquals(expiry, res.softHoldExpiresAt)
    }

    @Test
    fun `test hard reservation creation assigns HARD mode and attaches physical allocation source`() {
        val alloc = SubstrateAllocationSource(
            allocationId = "ALOC-001",
            reservationId = "SRES-002",
            tenantId = "TENANT-001",
            warehouseId = "WH-MAIN-01",
            locationId = "BAY-A3",
            batchNumber = "BATCH-2026-09",
            allocatedSheets = 5000L,
            allocatedReams = BigDecimal("10.0000"),
            allocatedWeightKg = BigDecimal("50.0000"),
            allocatedBy = "planner"
        )

        val res = SubstrateReservation(
            reservationId = "SRES-002",
            tenantId = "TENANT-001",
            orderId = "ORD-002",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-001",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            warehouseId = "WH-MAIN-01",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            reservedSheets = 5000L,
            reservedReams = BigDecimal("10.0000"),
            reservedWeightKg = BigDecimal("50.0000"),
            status = SubstrateReservationStatus.ALLOCATED_HARD,
            mode = SubstrateReservationMode.HARD,
            idempotencyKey = "key-002",
            reservedBy = "planner",
            allocationSources = listOf(alloc)
        )

        assertEquals(SubstrateReservationMode.HARD, res.mode)
        assertEquals(SubstrateReservationStatus.ALLOCATED_HARD, res.status)
        assertEquals(1, res.allocationSources.size)
        assertEquals("WH-MAIN-01", res.allocationSources[0].warehouseId)
        assertEquals("BATCH-2026-09", res.allocationSources[0].batchNumber)
        assertEquals(5000L, res.allocationSources[0].allocatedSheets)
    }

    @Test
    fun `test real-time available stock formula accurately subtracts active soft and hard holds`() {
        val totalOnHandPhysical = 20000L
        val softHoldSheets = 4000L
        val hardAllocatedSheets = 6000L
        val totalActiveHolds = softHoldSheets + hardAllocatedSheets

        val available = SubstrateReservationMathUtils.calculateAvailableStock(totalOnHandPhysical, totalActiveHolds)
        assertEquals(10000L, available)

        // If another request asks for 12,000 sheets -> it must exceed available
        val requestedDemand = 12000L
        assertTrue("Demand ($requestedDemand) must exceed available ($available)", requestedDemand > available)
    }
}

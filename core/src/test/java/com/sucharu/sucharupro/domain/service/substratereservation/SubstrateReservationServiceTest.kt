package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateRequirement
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateReservationServiceTest {

    private lateinit var service: SubstrateReservationService
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        val fakeDs = FakeSubstrateReservationDataSource()
        val repo = SubstrateReservationRepositoryImpl(fakeDs)
        service = SubstrateReservationServiceImpl(repo)
    }

    @Test
    fun `test complete substrate reservation lifecycle, hard allocation, and AI handoff export`() = runBlocking {
        val req = SubstrateRequirement(
            requirementId = "REQ-001",
            tenantId = tenantId,
            orderId = "ORD-101",
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
            totalWeightKg = BigDecimal("87.0966")
        )

        // 1. Create soft reservation
        val reservation = service.createReservation(
            tenantId = tenantId,
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            warehouseId = "WH-MAIN-01",
            requirement = req,
            isHardAllocation = false,
            actor = "estimator-1"
        )

        assertNotNull(reservation)
        assertEquals(SubstrateReservationStatus.RESERVED_SOFT, reservation.status)
        assertEquals(5000L, reservation.reservedSheets)

        // 2. Promote to hard allocation for scheduled job
        val hardAllocated = service.allocateHardForJob(
            tenantId = tenantId,
            reservationId = reservation.reservationId,
            executionJobId = "JOB-EXEC-101",
            workOrderId = "WO-01",
            actor = "production-scheduler"
        )

        assertEquals(SubstrateReservationStatus.ALLOCATED_HARD, hardAllocated.status)
        assertEquals("JOB-EXEC-101", hardAllocated.executionJobId)

        // 3. Export AI Handoff Contract
        val handoff = service.exportHandoffContract(tenantId, reservation.reservationId)
        assertEquals("1.0.0", handoff.contractVersion)
        assertTrue(handoff.isHardAllocated)
        assertTrue(handoff.isStockInterlocked)
        assertEquals("ART-300-25X36", handoff.sku)
    }

    @Test
    fun `test idempotency returns existing reservation when nonce matches`() = runBlocking {
        val req = SubstrateRequirement(
            requirementId = "REQ-001",
            tenantId = tenantId,
            orderId = "ORD-101",
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
            totalWeightKg = BigDecimal("87.0966")
        )

        val res1 = service.createReservation(
            tenantId = tenantId,
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            warehouseId = "WH-MAIN-01",
            requirement = req,
            actor = "estimator-1"
        )

        val res2 = service.createReservation(
            tenantId = tenantId,
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            warehouseId = "WH-MAIN-01",
            requirement = req,
            actor = "estimator-1"
        )

        assertEquals(res1.reservationId, res2.reservationId)
        assertEquals(res1.idempotencyKey, res2.idempotencyKey)
    }
}

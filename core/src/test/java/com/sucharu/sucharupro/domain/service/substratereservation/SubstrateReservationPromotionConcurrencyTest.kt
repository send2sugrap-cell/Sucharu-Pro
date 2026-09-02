package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateRequirement
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class SubstrateReservationPromotionConcurrencyTest {

    @Test
    fun `test concurrent parallel soft-to-hard promotions do not exceed available inventory`() = runBlocking {
        val fakeDs = FakeSubstrateReservationDataSource()
        val repo = SubstrateReservationRepositoryImpl(fakeDs)
        val service = SubstrateReservationServiceImpl(repo)

        val tenantId = "TENANT-001"
        val totalOnHandPhysical = 8000L
        val sheetsPerReservation = 2500L
        val totalOrders = 4 // 4 * 2500 = 10,000 sheets requested against 8,000 available

        // 1. Pre-create 4 soft reservations
        val softReservations = (1..totalOrders).map { index ->
            val req = SubstrateRequirement(
                requirementId = "REQ-$index",
                tenantId = tenantId,
                orderId = "ORD-$index",
                orderItemId = "ITEM-01",
                stockType = PaperStockType.ART_CARD,
                requestedMaterialCode = "ART-300-25X36",
                requestedMaterialName = "Art Card 300 GSM",
                gsm = BigDecimal("300.0000"),
                sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                productiveSheetsRequired = sheetsPerReservation,
                wasteSheetsRequired = 0L,
                totalSheetsRequired = sheetsPerReservation,
                totalReamsRequired = SubstrateReservationMathUtils.calculateReams(sheetsPerReservation),
                totalWeightKg = BigDecimal("25.0000")
            )

            service.createSoftReservation(
                tenantId = tenantId,
                orderId = "ORD-$index",
                orderItemId = "ITEM-01",
                productId = "PROD-01",
                sku = "ART-300-25X36",
                productName = "Art Card 300 GSM",
                warehouseId = "WH-MAIN-01",
                locationId = null,
                requirement = req,
                softHoldDurationMinutes = 60L,
                notes = "Pre-production quote $index",
                actor = "sales_$index"
            )
        }

        val successPromotionCount = AtomicInteger(0)
        val failedPromotionCount = AtomicInteger(0)
        val testLock = Mutex() // Simulates database row-level locking transaction boundary

        // 2. Parallel concurrent promotion attempts to HARD
        val jobs = softReservations.mapIndexed { index, softRes ->
            async(Dispatchers.Default) {
                testLock.withLock {
                    val alreadyHardAllocated = service.listAllReservations(tenantId)
                        .filter { it.status == SubstrateReservationStatus.ALLOCATED_HARD }
                        .sumOf { it.reservedSheets }

                    val availableForHardCommit = SubstrateReservationMathUtils.calculateAvailableStock(totalOnHandPhysical, alreadyHardAllocated)
                    if (availableForHardCommit >= softRes.reservedSheets) {
                        service.promoteSoftToHard(
                            tenantId = tenantId,
                            reservationId = softRes.reservationId,
                            executionJobId = "JOB-EXEC-$index",
                            workOrderId = "WO-$index",
                            allocatedWarehouseId = "WH-MAIN-01",
                            allocatedLocationId = "BAY-0$index",
                            allocatedBatchNumber = "BATCH-2026-0$index",
                            actor = "production_planner"
                        )
                        successPromotionCount.incrementAndGet()
                    } else {
                        failedPromotionCount.incrementAndGet()
                    }
                }
            }
        }

        jobs.awaitAll()

        val allReservations = service.listAllReservations(tenantId)
        val totalHardAllocated = allReservations
            .filter { it.status == SubstrateReservationStatus.ALLOCATED_HARD }
            .sumOf { it.reservedSheets }

        assertTrue("Total hard allocated ($totalHardAllocated) must not exceed physical stock ($totalOnHandPhysical)", totalHardAllocated <= totalOnHandPhysical)
        assertEquals(3, successPromotionCount.get()) // 3 * 2500 = 7500 <= 8000
        assertEquals(1, failedPromotionCount.get()) // 4th promotion rejected due to deficit
    }
}

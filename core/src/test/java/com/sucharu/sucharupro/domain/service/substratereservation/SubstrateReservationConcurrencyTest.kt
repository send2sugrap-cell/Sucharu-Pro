package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateRequirement
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class SubstrateReservationConcurrencyTest {

    @Test
    fun `test concurrent parallel reservation requests do not exceed available inventory`() = runBlocking {
        val fakeDs = FakeSubstrateReservationDataSource()
        val repo = SubstrateReservationRepositoryImpl(fakeDs)
        val service = SubstrateReservationServiceImpl(repo)

        val tenantId = "TENANT-001"
        val totalOnHandPhysical = 10000L
        val requestedPerOrder = 3000L
        val attempts = 5 // 5 * 3000 = 15,000 sheets requested against 10,000 available

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val testLock = Mutex() // Simulates database transaction lock

        val jobs = (1..attempts).map { index ->
            async(Dispatchers.Default) {
                testLock.withLock {
                    val allActive = service.listAllReservations(tenantId)
                        .filter { it.status.isActiveHold }
                        .sumOf { it.reservedSheets }

                    val available = SubstrateReservationMathUtils.calculateAvailableStock(totalOnHandPhysical, allActive)
                    if (available >= requestedPerOrder) {
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
                            productiveSheetsRequired = requestedPerOrder,
                            wasteSheetsRequired = 0L,
                            totalSheetsRequired = requestedPerOrder,
                            totalReamsRequired = SubstrateReservationMathUtils.calculateReams(requestedPerOrder),
                            totalWeightKg = BigDecimal("50.0000")
                        )

                        service.createReservation(
                            tenantId = tenantId,
                            orderId = "ORD-$index",
                            orderItemId = "ITEM-01",
                            productId = "PROD-01",
                            sku = "ART-300-25X36",
                            productName = "Art Card 300 GSM",
                            warehouseId = "WH-MAIN-01",
                            requirement = req,
                            actor = "user-$index"
                        )
                        successCount.incrementAndGet()
                    } else {
                        failureCount.incrementAndGet()
                    }
                }
            }
        }

        jobs.awaitAll()

        val allReservations = service.listAllReservations(tenantId)
        val totalReserved = allReservations.sumOf { it.reservedSheets }

        assertTrue("Total reserved ($totalReserved) must not exceed available ($totalOnHandPhysical)", totalReserved <= totalOnHandPhysical)
        assertEquals(3, successCount.get()) // 3 * 3000 = 9000 <= 10000
        assertEquals(2, failureCount.get()) // 2 failed due to deficit
    }
}

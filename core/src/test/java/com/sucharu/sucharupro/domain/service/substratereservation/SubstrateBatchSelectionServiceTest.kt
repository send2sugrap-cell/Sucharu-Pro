package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateBatchSelectionDataSource
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateBatchSelectionRepositoryImpl
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

/**
 * Service-level integration tests for SubstrateBatchSelectionService (Module 19 Step 03).
 */
class SubstrateBatchSelectionServiceTest {

    private lateinit var selectionRepository: SubstrateBatchSelectionRepositoryImpl
    private lateinit var selectionService: SubstrateBatchSelectionService
    private lateinit var reservationService: SubstrateReservationService

    private val tenantId = "TENANT-SERVICE-001"

    @Before
    fun setUp() {
        val fakeSelectionDs = FakeSubstrateBatchSelectionDataSource()
        selectionRepository = SubstrateBatchSelectionRepositoryImpl(fakeSelectionDs)
        selectionService = SubstrateBatchSelectionServiceImpl(selectionRepository)

        val fakeResDs = FakeSubstrateReservationDataSource()
        val resRepo = SubstrateReservationRepositoryImpl(fakeResDs)
        reservationService = SubstrateReservationServiceImpl(resRepo)
    }

    private fun createSampleSpec(reservationId: String? = null): BatchLotSelectionSpecification {
        return BatchLotSelectionSpecification(
            selectionId = "SBS-SVC-001",
            tenantId = tenantId,
            orderId = "ORD-SVC-99",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-SVC-10",
            reservationId = reservationId,
            productId = "PROD-ART-300",
            sku = "SKU-ART-300-25X36",
            requestedMaterialName = "Art Card 300 GSM",
            stockType = PaperStockType.ART_CARD,
            targetGsm = BigDecimal("300.0000"),
            requiredSheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            requiredGrainDirection = PaperGrainDirection.LONG_GRAIN,
            requiredSheets = 2500L,
            actor = "PLANNER_ALICE"
        )
    }

    private fun createCandidates(): List<BatchLotInventoryCandidate> {
        return listOf(
            BatchLotInventoryCandidate(
                candidateId = "CAND-01",
                tenantId = tenantId,
                warehouseId = "WH-01",
                warehouseName = "Central WH",
                locationId = "LOC-A",
                productId = "PROD-ART-300",
                sku = "SKU-ART-300-25X36",
                productName = "Art Card 300 GSM",
                batchNumber = "B-001",
                lotNumber = "L-001",
                stockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                grainDirection = PaperGrainDirection.LONG_GRAIN,
                onHandPhysicalSheets = 5000L,
                usableSheets = 4000L
            )
        )
    }

    @Test
    fun testEvaluateAndSaveSelection_Success() = runBlocking {
        val spec = createSampleSpec()
        val candidates = createCandidates()

        val result = selectionService.evaluateAndSelectBatches(spec, candidates)

        assertNotNull(result)
        assertEquals("SBS-SVC-001", result.selectionId)
        assertEquals(BatchLotSelectionStatus.FULLY_SATISFIED, result.status)
        assertEquals(2500L, result.allocatedSheets)

        // Verify retrieval from repository
        val fetched = selectionService.getSelectionResult(tenantId, "SBS-SVC-001")
        assertNotNull(fetched)
        assertEquals(2500L, fetched!!.allocatedSheets)
    }

    @Test
    fun testConfirmSelection_InterlocksWithReservationService() = runBlocking {
        // Step 1: Create a soft reservation in Step 02
        val req = SubstrateRequirement(
            requirementId = "REQ-01",
            tenantId = tenantId,
            orderId = "ORD-SVC-99",
            orderItemId = "ITEM-01",
            stockType = PaperStockType.ART_CARD,
            requestedMaterialName = "Art Card 300 GSM",
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            productiveSheetsRequired = 2000L,
            wasteSheetsRequired = 500L,
            totalSheetsRequired = 2500L,
            totalReamsRequired = BigDecimal("5.0000"),
            totalWeightKg = BigDecimal("435.5000")
        )

        val softRes = reservationService.createSoftReservation(
            tenantId = tenantId,
            orderId = "ORD-SVC-99",
            orderItemId = "ITEM-01",
            productId = "PROD-ART-300",
            sku = "SKU-ART-300-25X36",
            productName = "Art Card 300 GSM",
            warehouseId = "WH-01",
            requirement = req,
            actor = "TEST_USER"
        )

        // Step 2: Evaluate batch selection linked to reservation
        val spec = createSampleSpec(reservationId = softRes.reservationId)
        val candidates = createCandidates()
        selectionService.evaluateAndSelectBatches(spec, candidates)

        // Step 3: Confirm selection with reservation interlock
        val confirmed = selectionService.confirmSelectionAndAllocate(
            tenantId = tenantId,
            selectionId = "SBS-SVC-001",
            reservationService = reservationService,
            actor = "SUPERVISOR_BOB"
        )

        assertTrue(confirmed.isConfirmedAndAllocated)
        assertEquals("SUPERVISOR_BOB", confirmed.confirmedBy)

        // Step 4: Verify that reservation in Step 02 received the allocation sources
        val updatedRes = reservationService.getReservation(tenantId, softRes.reservationId)
        assertNotNull(updatedRes)
        assertEquals(1, updatedRes!!.allocationSources.size)
        assertEquals("B-001", updatedRes.allocationSources.first().batchNumber)
        assertEquals(2500L, updatedRes.allocationSources.first().allocatedSheets)
    }

    @Test
    fun testExportHandoffContract_EmitsValidV3Contract() = runBlocking {
        val spec = createSampleSpec()
        val candidates = createCandidates()
        selectionService.evaluateAndSelectBatches(spec, candidates)

        val handoff = selectionService.exportHandoffContract(tenantId, "SBS-SVC-001")

        assertEquals("3.0.0", handoff.contractVersion)
        assertEquals("SBS-SVC-001", handoff.selectionId)
        assertEquals("FULLY_SATISFIED", handoff.status)
        assertEquals(2500L, handoff.allocatedSheets)
        assertEquals(1, handoff.selectedBatchCount)
        assertTrue(handoff.masterIntegrityHash.isNotBlank())
    }

    @Test
    fun testListSelectionsByOrderAndJob() = runBlocking {
        val spec = createSampleSpec()
        val candidates = createCandidates()
        selectionService.evaluateAndSelectBatches(spec, candidates)

        val byOrder = selectionService.listSelectionsByOrder(tenantId, "ORD-SVC-99")
        assertEquals(1, byOrder.size)

        val byJob = selectionService.listSelectionsByJob(tenantId, "JOB-SVC-10")
        assertEquals(1, byJob.size)
    }
}

package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.substratereservation.EvaluateBatchSelectionRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateBatchSelectionDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateBatchSelectionRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.BatchLotInventoryCandidate
import com.sucharu.sucharupro.domain.model.substratereservation.BatchLotSelectionSpecification
import com.sucharu.sucharupro.domain.model.substratereservation.PaperGrainDirection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Security and multi-tenant edge tests for Substrate Batch/Lot Selection (Module 19 Step 03).
 */
class SubstrateBatchSelectionSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var selectionService: SubstrateBatchSelectionService
    private val fakeDs = FakeSubstrateBatchSelectionDataSource()

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"

    private val customerPrincipalA = AuthenticatedPrincipal(
        userId = "USR-CUST-A",
        projectId = tenantA,
        username = "cust_a",
        role = UserRole.CUSTOMER
    )

    private val vendorPrincipalA = AuthenticatedPrincipal(
        userId = "USR-VEND-A",
        projectId = tenantA,
        username = "vend_a",
        role = UserRole.VENDOR
    )

    @Before
    fun setUp() {
        val repo = SubstrateBatchSelectionRepositoryImpl(fakeDs)
        selectionService = SubstrateBatchSelectionServiceImpl(repo)
        val mockDb = MockPostgresEventDatabase()

        val repoFactory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantA
        ) {
            override fun createSubstrateBatchSelectionDataSource(tenantId: String) = fakeDs
            override fun createSubstrateBatchSelectionRepository(tenantId: String) = repo
            override fun createSubstrateBatchSelectionService(tenantId: String) = selectionService
        }

        useCases = BackendUseCases(mockDb, repoFactory)
    }

    @Test
    fun testCustomerAndVendorRoles_StrictlyDenied() {
        val reqDto = EvaluateBatchSelectionRequestDto(
            orderId = "ORD-001",
            orderItemId = "ITEM-001",
            productId = "PROD-001",
            sku = "SKU-001",
            requestedMaterialName = "Art Card 300 GSM",
            targetGsm = BigDecimal("300.0000"),
            sheetWidthMm = BigDecimal("635.0000"),
            sheetHeightMm = BigDecimal("914.4000"),
            requiredSheets = 1000L
        )

        assertThrows(ForbiddenException::class.java) {
            runBlocking { useCases.evaluateBatchLotSelection(customerPrincipalA, reqDto) }
        }

        assertThrows(ForbiddenException::class.java) {
            runBlocking { useCases.evaluateBatchLotSelection(vendorPrincipalA, reqDto) }
        }
    }

    @Test
    fun testMultiTenantIsolation_NoCrossTenantCandidateAccess() {
        // Candidate belongs to Tenant B
        val candidateTenantB = BatchLotInventoryCandidate(
            candidateId = "CAND-B-01",
            tenantId = tenantB,
            warehouseId = "WH-B",
            warehouseName = "Tenant B Warehouse",
            productId = "PROD-B",
            sku = "SKU-B",
            productName = "Tenant B Stock",
            batchNumber = "BATCH-B-01",
            lotNumber = "LOT-B-01",
            stockType = PaperStockType.ART_CARD,
            gsm = BigDecimal("300.0000"),
            sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            grainDirection = PaperGrainDirection.LONG_GRAIN,
            onHandPhysicalSheets = 5000L,
            usableSheets = 5000L
        )

        val specTenantA = BatchLotSelectionSpecification(
            selectionId = "SBS-SEC-01",
            tenantId = tenantA,
            orderId = "ORD-A-01",
            orderItemId = "ITEM-A-01",
            productId = "PROD-A",
            sku = "SKU-A",
            requestedMaterialName = "Art Card",
            stockType = PaperStockType.ART_CARD,
            targetGsm = BigDecimal("300.0000"),
            requiredSheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
            requiredGrainDirection = PaperGrainDirection.LONG_GRAIN,
            requiredSheets = 1000L
        )

        // Pass Tenant B candidate to Tenant A evaluation
        val result = runBlocking { selectionService.evaluateAndSelectBatches(specTenantA, listOf(candidateTenantB)) }

        // Tenant A must filter out Tenant B candidate and report 0 allocated sheets
        assertEquals(0L, result.allocatedSheets)
        assertEquals(0, result.selectedBatches.size)
        assertFalse(result.isFullySatisfied)
    }

    @Test
    fun testNegativeOrZeroQuantity_ThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            val spec = BatchLotSelectionSpecification(
                selectionId = "SBS-ERR-01",
                tenantId = tenantA,
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productId = "P1",
                sku = "S1",
                requestedMaterialName = "M1",
                stockType = PaperStockType.ART_CARD,
                targetGsm = BigDecimal("300.0000"),
                requiredSheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                requiredGrainDirection = PaperGrainDirection.LONG_GRAIN,
                requiredSheets = 0L
            )
            BatchLotSelectionEngine.selectBatches(spec, emptyList())
        }
    }

    @Test
    fun testBlankTenantOrOrder_ThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            val spec = BatchLotSelectionSpecification(
                selectionId = "SBS-ERR-02",
                tenantId = "",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                productId = "P1",
                sku = "S1",
                requestedMaterialName = "M1",
                stockType = PaperStockType.ART_CARD,
                targetGsm = BigDecimal("300.0000"),
                requiredSheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                requiredGrainDirection = PaperGrainDirection.LONG_GRAIN,
                requiredSheets = 1000L
            )
            BatchLotSelectionEngine.selectBatches(spec, emptyList())
        }
    }
}

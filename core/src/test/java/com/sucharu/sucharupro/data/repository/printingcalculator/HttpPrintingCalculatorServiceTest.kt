package com.sucharu.sucharupro.data.repository.printingcalculator

import com.sucharu.sucharupro.data.api.MockApiConnectionProvider
import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.auth.TestSecurityFixtures
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Real API integration test for HttpPrintingCalculatorService and BackendRouter endpoints (Phase 03 Step 01).
 * Verifies end-to-end calculation workflow over the REST API boundary.
 */
class HttpPrintingCalculatorServiceTest {

    private lateinit var client: DirectBackendApiClient
    private lateinit var httpCalculatorService: HttpPrintingCalculatorService

    @Before
    fun setUp() = runBlocking {
        val mockProvider = MockApiConnectionProvider()
        val transactionManager = DefaultPostgresTransactionManager(mockProvider)
        val fakeDataSource = com.sucharu.sucharupro.data.datasource.printingcalculator.FakePrintingCalculatorDataSource()
        val repositoryFactory = object : PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001") {
            override fun createPrintingCalculatorDataSource(tenantId: String): com.sucharu.sucharupro.data.datasource.printingcalculator.PrintingCalculatorDataSource {
                return fakeDataSource
            }
        }
        val securityContext = BackendSecurityContext()
        TestSecurityFixtures.registerStandardTestTokens(securityContext)

        val server = BackendApiServer(
            connectionProvider = mockProvider,
            transactionManager = transactionManager,
            repositoryFactory = repositoryFactory,
            securityContext = securityContext
        )
        server.start()

        val tokenStorage = InMemoryAuthTokenStorage()
        tokenStorage.saveToken("token-customer-100")
        client = DirectBackendApiClient(server = server, tokenStorage = tokenStorage)

        httpCalculatorService = HttpPrintingCalculatorService(client = client)
    }

    @Test
    fun testHttpCalculate_executesAuthoritativeCalculationOverApi() = runBlocking {
        val request = PrintingCalculationRequest(
            tenantId = "TENANT-001",
            projectId = "PROJECT-001",
            jobTitle = "Flyer Printing 5000",
            quantity = 5000L,
            finishedWidth = BigDecimal("210.0000"),
            finishedHeight = BigDecimal("297.0000"),
            materialName = "Art Paper 150 GSM",
            stockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000"),
            sheetWidth = BigDecimal("635.0000"),
            sheetHeight = BigDecimal("914.0000"),
            materialUnitPricePerSheet = BigDecimal("10.0000"),
            processType = PrintingProcessType.OFFSET,
            sides = PrintingSideOption.DOUBLE_SIDED_SAME,
            colorMode = ColorMode.CMYK_FOUR_COLOR,
            frontColorsCount = 4,
            backColorsCount = 4,
            machine = MachineSpecification(
                machineName = "Heidelberg Speedmaster 4-Color",
                processType = PrintingProcessType.OFFSET,
                hourlyRate = BigDecimal("2500.0000"),
                impressionsPerHour = 8000,
                plateCostPerUnit = BigDecimal("450.0000")
            )
        )

        val result = httpCalculatorService.calculate(request)
        if (result is DomainResult.Error) {
            throw RuntimeException("HTTP CALCULATE FAILED: ${(result as DomainResult.Error).message}")
        }

        val calcResult = (result as DomainResult.Success).data
        assertEquals("TENANT-001", calcResult.tenantId)
        assertEquals(CalculationStatus.SUCCESSFUL, calcResult.status)
        assertNotNull(calcResult.totalEstimatedCost)
        assertNotNull(calcResult.estimatedUnitCost)
        assertTrue(calcResult.breakdownItems.isNotEmpty())

        // Verify retrieval by ID over HTTP
        val fetchById = httpCalculatorService.getCalculationById("TENANT-001", calcResult.calculationId)
        if (fetchById is DomainResult.Error) throw RuntimeException("FETCH BY ID FAILED: ${fetchById.message}")
        assertTrue("Get by ID over HTTP should succeed", fetchById is DomainResult.Success)
        val fetchedData = (fetchById as DomainResult.Success).data
        assertNotNull(fetchedData)
        assertEquals(calcResult.calculationId, fetchedData?.calculationId)

        // Verify breakdown over HTTP
        val breakdown = httpCalculatorService.getCalculationBreakdown("TENANT-001", calcResult.calculationId)
        if (breakdown is DomainResult.Error) throw RuntimeException("GET BREAKDOWN FAILED: ${breakdown.message}")
        assertTrue("Get breakdown over HTTP should succeed", breakdown is DomainResult.Success)
        val items = (breakdown as DomainResult.Success).data
        assertTrue(items.isNotEmpty())

        // Verify handoff contract export over HTTP
        val handoff = httpCalculatorService.exportHandoffContract("TENANT-001", calcResult.calculationId)
        if (handoff is DomainResult.Error) throw RuntimeException("EXPORT HANDOFF FAILED: ${handoff.message}")
        assertTrue("Export handoff over HTTP should succeed", handoff is DomainResult.Success)
        val contract = (handoff as DomainResult.Success).data
        assertEquals(calcResult.calculationId, contract.calculationId)
        assertNotNull(contract.handoffIntegrityHash)
    }

    @Test
    fun testHttpValidateRequest_returnsDiagnosticsOverApi() = runBlocking {
        val request = PrintingCalculationRequest(
            tenantId = "TENANT-001",
            projectId = "PROJECT-001",
            jobTitle = "Invalid Size Job",
            quantity = 100L,
            finishedWidth = BigDecimal("2000.0000"),
            finishedHeight = BigDecimal("2000.0000"),
            materialName = "Standard Paper"
        )

        val valResult = httpCalculatorService.validateRequest(request)
        assertTrue("HTTP validation should return Success", valResult is DomainResult.Success)
        val validation = (valResult as DomainResult.Success).data
        assertNotNull(validation.diagnostics)
    }
}

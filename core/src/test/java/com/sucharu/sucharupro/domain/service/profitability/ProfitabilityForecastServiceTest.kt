package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.api.model.profitability.ProfitabilityGenerateForecastRequestDto
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.profitability.FakeProfitabilityForecastDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityForecastRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Comprehensive Automated Test Suite for Profitability Forecasting & Scenario Engine.
 * Module 16 Step 08.
 */
class ProfitabilityForecastServiceTest {

    private lateinit var dataSource: FakeProfitabilityForecastDataSource
    private lateinit var repository: ProfitabilityForecastRepositoryImpl
    private lateinit var sourceCollector: ProfitabilityForecastSourceCollector
    private lateinit var service: ProfitabilityForecastServiceImpl
    private lateinit var reconciliationService: ProfitabilityForecastReconciliationServiceImpl
    private lateinit var scenarioEngine: ProfitabilityScenarioEngineImpl

    private val tenantId = "tenant-test-01"
    private val projectId = "project-test-01"

    @Before
    fun setUp() {
        dataSource = FakeProfitabilityForecastDataSource()
        repository = ProfitabilityForecastRepositoryImpl(dataSource)
        reconciliationService = ProfitabilityForecastReconciliationServiceImpl()
        scenarioEngine = ProfitabilityScenarioEngineImpl()

        sourceCollector = object : ProfitabilityForecastSourceCollector {
            override suspend fun collectHistoricalSeries(
                tenantId: String,
                projectId: String,
                scope: ProfitabilityForecastScope,
                targetEntityId: String,
                historicalPeriodStart: String,
                historicalPeriodEnd: String
            ): DomainResult<HistoricalProfitabilitySeries> {
                return DomainResult.Success(
                    HistoricalProfitabilitySeries(
                        periods = listOf("2026-M01", "2026-M02", "2026-M03", "2026-M04"),
                        revenues = listOf(
                            BigDecimal("100000.0000"),
                            BigDecimal("110000.0000"),
                            BigDecimal("120000.0000"),
                            BigDecimal("130000.0000")
                        ),
                        costs = listOf(
                            BigDecimal("70000.0000"),
                            BigDecimal("75000.0000"),
                            BigDecimal("80000.0000"),
                            BigDecimal("85000.0000")
                        ),
                        grossProfits = listOf(
                            BigDecimal("30000.0000"),
                            BigDecimal("35000.0000"),
                            BigDecimal("40000.0000"),
                            BigDecimal("45000.0000")
                        ),
                        units = listOf(1000L, 1100L, 1200L, 1300L),
                        componentAverages = mapOf(
                            JobCostComponentType.MATERIAL_COST to BigDecimal("30000.0000"),
                            JobCostComponentType.LABOUR_COST to BigDecimal("15000.0000"),
                            JobCostComponentType.MACHINE_COST to BigDecimal("15000.0000"),
                            JobCostComponentType.VENDOR_OUTSOURCE_COST to BigDecimal("5000.0000"),
                            JobCostComponentType.ALLOCATED_INDIRECT_COST to BigDecimal("2500.0000")
                        ),
                        isReconciled = true,
                        sourceReadiness = PeriodSourceReadiness.READY
                    )
                )
            }
        }

        service = ProfitabilityForecastServiceImpl(
            repository = repository,
            sourceCollector = sourceCollector,
            forecastEngine = ProfitabilityForecastEngineImpl(),
            scenarioEngine = scenarioEngine,
            reconciliationService = reconciliationService
        )
    }

    @Test
    fun testMathematicalPrecisionAndCoreIdentities() = runBlocking {
        val result = service.generateForecast(
            tenantId = tenantId,
            projectId = projectId,
            targetScope = ProfitabilityForecastScope.BUSINESS,
            targetEntityId = "ALL",
            targetEntityLabel = "Commercial Printing Operations",
            historicalPeriodStart = "2026-M01",
            historicalPeriodEnd = "2026-M04",
            forecastPeriodStart = "2026-M05",
            forecastPeriodEnd = "2026-M05",
            horizon = ForecastHorizon.NEXT_1_PERIOD,
            forecastMethod = ProfitabilityForecastMethod.ROLLING_AVERAGE,
            scenarioType = ProfitabilityScenarioType.BASELINE,
            scenarioId = null,
            idempotencyKey = "key-001",
            actorId = "admin-1",
            actorRole = "ADMIN"
        )

        assertTrue(result is DomainResult.Success)
        val snap = (result as DomainResult.Success).data

        // 1. Math scale & rounding
        assertEquals(4, snap.projectedRevenue.scale())
        assertEquals(4, snap.projectedTotalCost.scale())
        assertEquals(4, snap.projectedGrossProfit.scale())

        // 2. Identity: Revenue - Cost == Gross Profit
        val expectedProfit = snap.projectedRevenue.subtract(snap.projectedTotalCost)
        assertEquals(0, snap.projectedGrossProfit.compareTo(expectedProfit))

        // 3. Components Sum == Total Cost
        val compSum = snap.components.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.projectedAmount) }
        assertEquals(0, snap.projectedTotalCost.compareTo(compSum))

        // 4. Reconciliation
        val recon = service.reconcileForecast(tenantId, projectId, snap.forecastId)
        assertTrue(recon is DomainResult.Success)
        assertTrue((recon as DomainResult.Success).data.isBalanced)
    }

    @Test
    fun testAllSixForecastingMethods() = runBlocking {
        val methods = listOf(
            ProfitabilityForecastMethod.HISTORICAL_BASELINE,
            ProfitabilityForecastMethod.ROLLING_AVERAGE,
            ProfitabilityForecastMethod.WEIGHTED_ROLLING_AVERAGE,
            ProfitabilityForecastMethod.TREND_BASED,
            ProfitabilityForecastMethod.DRIVER_BASED,
            ProfitabilityForecastMethod.SCENARIO_BASED
        )

        for (method in methods) {
            val result = service.generateForecast(
                tenantId = tenantId,
                projectId = projectId,
                targetScope = ProfitabilityForecastScope.CUSTOMER,
                targetEntityId = "CUST-001",
                targetEntityLabel = "Acme Corp",
                historicalPeriodStart = "2026-M01",
                historicalPeriodEnd = "2026-M04",
                forecastPeriodStart = "2026-M05",
                forecastPeriodEnd = "2026-M05",
                horizon = ForecastHorizon.NEXT_1_PERIOD,
                forecastMethod = method,
                scenarioType = ProfitabilityScenarioType.BASELINE,
                scenarioId = null,
                idempotencyKey = null,
                actorId = "manager-1",
                actorRole = "MANAGER"
            )

            assertTrue("Method $method failed", result is DomainResult.Success)
            val snap = (result as DomainResult.Success).data
            assertTrue(snap.projectedRevenue > BigDecimal.ZERO)
            assertTrue(snap.projectedTotalCost > BigDecimal.ZERO)
            assertTrue(snap.confidenceScore in BigDecimal.ZERO..BigDecimal("100.0000"))
        }
    }

    @Test
    fun testMultiHorizonProjections() = runBlocking {
        val h1 = service.generateForecast(tenantId, projectId, ProfitabilityForecastScope.BUSINESS, "ALL", "All", "2026-M01", "2026-M04", "2026-M05", "2026-M05", ForecastHorizon.NEXT_1_PERIOD, ProfitabilityForecastMethod.ROLLING_AVERAGE, ProfitabilityScenarioType.BASELINE, null, null, "admin", "ADMIN")
        val h3 = service.generateForecast(tenantId, projectId, ProfitabilityForecastScope.BUSINESS, "ALL", "All", "2026-M01", "2026-M04", "2026-M05", "2026-M07", ForecastHorizon.NEXT_3_PERIODS, ProfitabilityForecastMethod.ROLLING_AVERAGE, ProfitabilityScenarioType.BASELINE, null, null, "admin", "ADMIN")

        assertTrue(h1 is DomainResult.Success)
        assertTrue(h3 is DomainResult.Success)

        val snap1 = (h1 as DomainResult.Success).data
        val snap3 = (h3 as DomainResult.Success).data

        // 3-period projection should be approximately 3x of 1-period rolling average
        val ratio = snap3.projectedRevenue.divide(snap1.projectedRevenue, 2, RoundingMode.HALF_UP)
        assertEquals(0, ratio.compareTo(BigDecimal("3.00")))
    }

    @Test
    fun testScenarioModellingAndComparison() = runBlocking {
        val baseRes = service.generateForecast(
            tenantId = tenantId,
            projectId = projectId,
            targetScope = ProfitabilityForecastScope.PRODUCT,
            targetEntityId = "PROD-100",
            targetEntityLabel = "Hardcover Book",
            historicalPeriodStart = "2026-M01",
            historicalPeriodEnd = "2026-M04",
            forecastPeriodStart = "2026-M05",
            forecastPeriodEnd = "2026-M05",
            horizon = ForecastHorizon.NEXT_1_PERIOD,
            forecastMethod = ProfitabilityForecastMethod.ROLLING_AVERAGE,
            scenarioType = ProfitabilityScenarioType.BASELINE,
            scenarioId = null,
            idempotencyKey = null,
            actorId = "admin",
            actorRole = "ADMIN"
        )
        val baseSnap = (baseRes as DomainResult.Success).data

        // Create Custom Scenario: 10% Revenue Increase, 5% Material Cost Reduction
        val customScenario = ProfitabilityScenario(
            scenarioId = "scen-custom-01",
            tenantId = tenantId,
            projectId = projectId,
            scenarioName = "Price Increase & Material Efficiency",
            scenarioType = ProfitabilityScenarioType.CUSTOM,
            description = "10% price bump with improved paper purchasing",
            targetScope = ProfitabilityForecastScope.PRODUCT,
            revenueAdjustmentPercentage = BigDecimal("10.0000"),
            materialCostAdjustmentPercentage = BigDecimal("-5.0000"),
            createdAt = System.currentTimeMillis(),
            createdBy = "admin"
        )
        service.createScenario(tenantId, projectId, customScenario)

        // Compare Scenarios
        val compRes = service.compareScenarios(tenantId, projectId, baseSnap.forecastId, null)
        assertTrue(compRes is DomainResult.Success)

        val comp = (compRes as DomainResult.Success).data
        assertTrue(comp.comparedScenarios.isNotEmpty())
        val opt = comp.comparedScenarios.find { it.scenarioType == ProfitabilityScenarioType.OPTIMISTIC }
        assertNotNull(opt)
        assertTrue(opt!!.projectedGrossProfit > comp.baselineScenario.projectedGrossProfit)
    }

    @Test
    fun testForecastActualComparisonAndMape() = runBlocking {
        val baseRes = service.generateForecast(
            tenantId = tenantId,
            projectId = projectId,
            targetScope = ProfitabilityForecastScope.BUSINESS,
            targetEntityId = "ALL",
            targetEntityLabel = "All Operations",
            historicalPeriodStart = "2026-M01",
            historicalPeriodEnd = "2026-M04",
            forecastPeriodStart = "2026-M05",
            forecastPeriodEnd = "2026-M05",
            horizon = ForecastHorizon.NEXT_1_PERIOD,
            forecastMethod = ProfitabilityForecastMethod.ROLLING_AVERAGE,
            scenarioType = ProfitabilityScenarioType.BASELINE,
            scenarioId = null,
            idempotencyKey = null,
            actorId = "admin",
            actorRole = "ADMIN"
        )
        val snap = (baseRes as DomainResult.Success).data

        val actualRev = BigDecimal("120000.0000")
        val actualCost = BigDecimal("80000.0000")
        val compRes = service.compareWithActual(
            tenantId = tenantId,
            projectId = projectId,
            forecastId = snap.forecastId,
            actualRevenue = actualRev,
            actualCost = actualCost,
            actualUnits = 1200L,
            actualPeriodId = "2026-M05"
        )

        assertTrue(compRes is DomainResult.Success)
        val comp = (compRes as DomainResult.Success).data
        assertEquals(0, comp.actualRevenue.compareTo(actualRev))
        assertEquals(0, comp.actualGrossProfit.compareTo(BigDecimal("40000.0000")))
        assertTrue(comp.meanAbsolutePercentageError != null)
        assertTrue(comp.isDirectionallyAccurate)
    }

    @Test
    fun testConcurrencyAndIdempotency() = runBlocking {
        val req = ProfitabilityGenerateForecastRequestDto(
            targetScope = "BUSINESS",
            targetEntityId = "CONCURRENCY_TEST",
            targetEntityLabel = "Concurrency Target",
            historicalPeriodStart = "2026-M01",
            historicalPeriodEnd = "2026-M04",
            forecastPeriodStart = "2026-M05",
            forecastPeriodEnd = "2026-M05",
            horizon = "NEXT_1_PERIOD",
            forecastMethod = "ROLLING_AVERAGE",
            scenarioType = "BASELINE",
            idempotencyKey = "idemp-atomic-123"
        )

        val deferreds = (1..5).map {
            async {
                service.generateForecast(
                    tenantId = tenantId,
                    projectId = projectId,
                    targetScope = ProfitabilityForecastScope.BUSINESS,
                    targetEntityId = req.targetEntityId,
                    targetEntityLabel = req.targetEntityLabel,
                    historicalPeriodStart = req.historicalPeriodStart,
                    historicalPeriodEnd = req.historicalPeriodEnd,
                    forecastPeriodStart = req.forecastPeriodStart,
                    forecastPeriodEnd = req.forecastPeriodEnd,
                    horizon = ForecastHorizon.NEXT_1_PERIOD,
                    forecastMethod = ProfitabilityForecastMethod.ROLLING_AVERAGE,
                    scenarioType = ProfitabilityScenarioType.BASELINE,
                    scenarioId = null,
                    idempotencyKey = req.idempotencyKey,
                    actorId = "admin",
                    actorRole = "ADMIN"
                )
            }
        }

        val results = deferreds.awaitAll()
        val successfulForecastIds = results.mapNotNull {
            if (it is DomainResult.Success) it.data.forecastId else null
        }

        assertEquals(5, successfulForecastIds.size)
        // Idempotent calls must return the same forecastId
        val distinctIds = successfulForecastIds.distinct()
        assertEquals(1, distinctIds.size)
    }

    @Test
    fun testHandoffContractExport() = runBlocking {
        val res = service.generateForecast(
            tenantId = tenantId,
            projectId = projectId,
            targetScope = ProfitabilityForecastScope.BUSINESS,
            targetEntityId = "ALL",
            targetEntityLabel = "All Operations",
            historicalPeriodStart = "2026-M01",
            historicalPeriodEnd = "2026-M04",
            forecastPeriodStart = "2026-M05",
            forecastPeriodEnd = "2026-M05",
            horizon = ForecastHorizon.NEXT_1_PERIOD,
            forecastMethod = ProfitabilityForecastMethod.TREND_BASED,
            scenarioType = ProfitabilityScenarioType.BASELINE,
            scenarioId = null,
            idempotencyKey = null,
            actorId = "admin",
            actorRole = "ADMIN"
        )
        val snap = (res as DomainResult.Success).data

        val handoffRes = service.exportHandoffContract(tenantId, projectId, snap.forecastId)
        assertTrue(handoffRes is DomainResult.Success)

        val contract = (handoffRes as DomainResult.Success).data
        assertEquals("MODULE16_STEP08_V1", contract.contractVersion)
        assertEquals(snap.forecastId, contract.forecastId)
        assertEquals(snap.integrityHash, contract.integrityHash)
        assertTrue(contract.isReconciled)
        assertTrue(contract.topManagementInsights.isNotEmpty())
    }
}

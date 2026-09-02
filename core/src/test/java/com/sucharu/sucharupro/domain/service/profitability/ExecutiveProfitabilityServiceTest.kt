package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeExecutiveProfitabilityDataSource
import com.sucharu.sucharupro.data.repository.profitability.ExecutiveProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Comprehensive Unit and Integration Test Suite for Executive Profitability Engine.
 * Module 16 Step 10.
 */
class ExecutiveProfitabilityServiceTest {

    private val tenantId = "tenant-001"
    private val projectId = "tenant-001"

    private lateinit var fakeDataSource: FakeExecutiveProfitabilityDataSource
    private lateinit var repository: ExecutiveProfitabilityRepositoryImpl
    private lateinit var sourceCollector: FakeExecutiveProfitabilitySourceCollector
    private lateinit var service: ExecutiveProfitabilityServiceImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeExecutiveProfitabilityDataSource()
        repository = ExecutiveProfitabilityRepositoryImpl(fakeDataSource)
        sourceCollector = FakeExecutiveProfitabilitySourceCollector()
        service = ExecutiveProfitabilityServiceImpl(
            repository = repository,
            sourceCollector = sourceCollector
        )
    }

    @Test
    fun testCalculateSnapshot_computesKpisScorecardAndPersistsSnapshot() = runBlocking {
        // Setup payload with jobs, products, customers, and vendors
        sourceCollector.currentPayload = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-101",
                    jobCode = "JB-101",
                    customerId = "CUST-01",
                    revenue = BigDecimal("50000.0000"),
                    actualCost = BigDecimal("35000.0000"),
                    grossProfit = BigDecimal("15000.0000"),
                    grossMarginPercentage = BigDecimal("30.0000")
                ),
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-102",
                    jobCode = "JB-102",
                    customerId = "CUST-02",
                    revenue = BigDecimal("20000.0000"),
                    actualCost = BigDecimal("16000.0000"),
                    grossProfit = BigDecimal("4000.0000"),
                    grossMarginPercentage = BigDecimal("20.0000")
                )
            ),
            products = listOf(
                ProductProfitabilityEvaluationItem(
                    productId = "PROD-01",
                    productCode = "PKG-01",
                    productName = "Shipping Carton",
                    totalRevenue = BigDecimal("50000.0000"),
                    totalCost = BigDecimal("35000.0000"),
                    grossProfit = BigDecimal("15000.0000"),
                    grossMarginPercentage = BigDecimal("30.0000"),
                    unitCost = BigDecimal("35.0000"),
                    averageSellingPrice = BigDecimal("50.0000"),
                    totalUnits = 1000L
                )
            ),
            customers = listOf(
                CustomerProfitabilityEvaluationItem(
                    customerId = "CUST-01",
                    customerCode = "C-01",
                    customerName = "Apex Logistics",
                    totalRevenue = BigDecimal("50000.0000"),
                    totalCost = BigDecimal("35000.0000"),
                    grossProfit = BigDecimal("15000.0000"),
                    grossMarginPercentage = BigDecimal("30.0000"),
                    contributionMarginPercentage = BigDecimal("40.0000"),
                    revenueSharePercentage = BigDecimal("71.4286")
                )
            ),
            vendors = listOf(
                VendorProfitabilityEvaluationItem(
                    vendorId = "VEND-01",
                    vendorCode = "V-01",
                    vendorName = "Kraft Paper Mills Ltd",
                    totalSpend = BigDecimal("25000.0000"),
                    spendSharePercentage = BigDecimal("49.0196"),
                    costPressureScore = BigDecimal("45.0000"),
                    dependencyRiskScore = BigDecimal("50.0000")
                )
            )
        )

        val result = service.calculateSnapshot(tenantId, projectId, "2026-M09", null, "admin-1", "ADMIN")
        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data

        assertEquals(BigDecimal("70000.0000"), snapshot.totalGrossRevenue)
        assertEquals(BigDecimal("51000.0000"), snapshot.totalActualCost)
        assertEquals(BigDecimal("19000.0000"), snapshot.totalGrossProfit)
        assertEquals(BigDecimal("27.1429"), snapshot.grossMarginPercentage)
        assertTrue(snapshot.overallScore > BigDecimal("60.0000"))
        assertTrue(snapshot.integrityHash.isNotBlank())

        // Verify latest lookup retrieves saved snapshot
        val latestRes = service.getLatestSnapshot(tenantId, projectId, "2026-M09")
        assertTrue(latestRes is DomainResult.Success)
        assertEquals(snapshot.snapshotId, (latestRes as DomainResult.Success).data.snapshotId)
    }

    @Test
    fun testGetKpisAndScorecard_evaluatesTenDimensionsDeterministically() = runBlocking {
        sourceCollector.currentPayload = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-201",
                    jobCode = "JB-201",
                    customerId = "CUST-01",
                    revenue = BigDecimal("100000.0000"),
                    actualCost = BigDecimal("70000.0000"),
                    grossProfit = BigDecimal("30000.0000"),
                    grossMarginPercentage = BigDecimal("30.0000")
                )
            ),
            customers = listOf(
                CustomerProfitabilityEvaluationItem(
                    customerId = "CUST-01",
                    customerCode = "C-01",
                    customerName = "Primary Customer",
                    totalRevenue = BigDecimal("100000.0000"),
                    totalCost = BigDecimal("70000.0000"),
                    grossProfit = BigDecimal("30000.0000"),
                    grossMarginPercentage = BigDecimal("30.0000"),
                    contributionMarginPercentage = BigDecimal("35.0000"),
                    revenueSharePercentage = BigDecimal("100.0000")
                )
            )
        )

        val kpiRes = service.getKpis(tenantId, projectId, "2026-M09")
        assertTrue(kpiRes is DomainResult.Success)
        val kpis = (kpiRes as DomainResult.Success).data
        assertTrue(kpis.any { it.kpiKey == "REV_GROSS" && it.currentValue == BigDecimal("100000.0000") })
        assertTrue(kpis.any { it.kpiKey == "PROFIT_GROSS" && it.currentValue == BigDecimal("30000.0000") })
        assertTrue(kpis.any { it.kpiKey == "PROFIT_MARGIN_PCT" && it.currentValue == BigDecimal("30.0000") })

        val scRes = service.getScorecard(tenantId, projectId, "2026-M09")
        assertTrue(scRes is DomainResult.Success)
        val scorecard = (scRes as DomainResult.Success).data
        assertEquals(10, scorecard.items.size)
        assertTrue(scorecard.overallScore in BigDecimal("0.0000")..BigDecimal("100.0000"))
    }

    @Test
    fun testRankingsAndConcentration_computesTopEntitiesAndRisk() = runBlocking {
        sourceCollector.currentPayload = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem("J1", "JB-1", "C1", BigDecimal("50000.0000"), BigDecimal("30000.0000"), BigDecimal("20000.0000"), BigDecimal("40.0000")),
                JobProfitabilityEvaluationItem("J2", "JB-2", "C2", BigDecimal("10000.0000"), BigDecimal("12000.0000"), BigDecimal("-2000.0000"), BigDecimal("-20.0000"))
            ),
            customers = listOf(
                CustomerProfitabilityEvaluationItem("C1", "C-1", "Client One", BigDecimal("80000.0000"), BigDecimal("50000.0000"), BigDecimal("30000.0000"), BigDecimal("37.5000"), BigDecimal("45.0000"), BigDecimal("80.0000"))
            ),
            vendors = listOf(
                VendorProfitabilityEvaluationItem("V1", "V-1", "Paper Co", BigDecimal("60000.0000"), BigDecimal("85.0000"), BigDecimal("80.0000"), BigDecimal("90.0000"))
            )
        )

        val rankRes = service.getRankings(tenantId, projectId, "2026-M09")
        assertTrue(rankRes is DomainResult.Success)
        val rankings = (rankRes as DomainResult.Success).data
        assertEquals(1, rankings.topProfitableJobs.size)
        assertEquals(1, rankings.lossMakingJobs.size)

        val concRes = service.getConcentration(tenantId, projectId, "2026-M09")
        assertTrue(concRes is DomainResult.Success)
        val concentration = (concRes as DomainResult.Success).data
        assertEquals(BigDecimal("100.0000"), concentration.customerRevenueConcentration.top1SharePercentage)
        assertEquals(ForecastRiskLevel.VERY_HIGH, concentration.customerRevenueConcentration.riskLevel)
    }

    @Test
    fun testReconciliationAndCryptographicHandoffExport() = runBlocking {
        sourceCollector.currentPayload = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem("J1", "JB-1", "C1", BigDecimal("50000.0000"), BigDecimal("35000.0000"), BigDecimal("15000.0000"), BigDecimal("30.0000"))
            )
        )

        // Reconciliation
        val reconRes = service.getReconciliation(tenantId, projectId, "2026-M09")
        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data
        assertTrue(recon.isBalanced)
        assertTrue(recon.revenueMatches)
        assertTrue(recon.costMatches)
        assertTrue(recon.profitMatches)

        // Full Executive Report
        val repRes = service.getFullReport(tenantId, projectId, "2026-M09")
        assertTrue(repRes is DomainResult.Success)
        val report = (repRes as DomainResult.Success).data
        assertTrue(report.sections.isNotEmpty())
        assertEquals("1.0.0", report.contractVersion)

        // AI Handoff Contract
        val handoffRes = service.exportHandoffContract(tenantId, projectId, "2026-M09")
        assertTrue(handoffRes is DomainResult.Success)
        val contract = (handoffRes as DomainResult.Success).data
        assertTrue(contract.isReadOnly)
        assertEquals("1.0.0", contract.contractVersion)
        assertTrue(contract.handoffIntegrityHash.isNotBlank())
    }
}

/**
 * Fake Source Collector for Step 10 Unit Tests.
 */
class FakeExecutiveProfitabilitySourceCollector : ExecutiveProfitabilitySourceCollector {

    var currentPayload = ProfitabilityEvaluationPayload(
        tenantId = "tenant-001",
        projectId = "tenant-001",
        periodId = "2026-M09"
    )

    override suspend fun collectCurrentPayload(tenantId: String, projectId: String, periodId: String?): DomainResult<ProfitabilityEvaluationPayload> {
        return DomainResult.Success(currentPayload)
    }

    override suspend fun collectPreviousPayload(tenantId: String, projectId: String, periodId: String?): DomainResult<ProfitabilityEvaluationPayload?> {
        return DomainResult.Success(null)
    }

    override suspend fun collectForecastSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ProfitabilityForecastSnapshot?> {
        return DomainResult.Success(null)
    }

    override suspend fun collectAlertSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ProfitabilityMonitoringSnapshot?> {
        return DomainResult.Success(null)
    }

    override suspend fun collectActiveAlerts(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlert>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun collectManagementActions(tenantId: String, projectId: String): DomainResult<List<ProfitabilityManagementAction>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun collectLeakageSummary(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveLeakageSummary> {
        return DomainResult.Success(
            ExecutiveLeakageSummary(
                totalLeakageAmount = BigDecimal.ZERO.setScale(4),
                leakagePercentageOfRevenue = BigDecimal.ZERO.setScale(4),
                directMaterialWastageLeakage = BigDecimal.ZERO.setScale(4),
                reworkCostLeakage = BigDecimal.ZERO.setScale(4),
                unallocatedOverheadLeakage = BigDecimal.ZERO.setScale(4),
                pricingErosionLeakage = BigDecimal.ZERO.setScale(4),
                vendorCostSurgeLeakage = BigDecimal.ZERO.setScale(4),
                topLeakageItems = emptyList(),
                primaryMitigationRecommendation = "Zero detected cost leakage."
            )
        )
    }

    override suspend fun collectProfitabilityDrivers(tenantId: String, projectId: String, periodId: String?): DomainResult<List<ExecutiveProfitabilityDriver>> {
        return DomainResult.Success(emptyList())
    }
}

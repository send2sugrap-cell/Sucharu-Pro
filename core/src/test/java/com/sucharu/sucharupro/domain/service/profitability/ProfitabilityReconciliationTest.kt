package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.businessintegrity.Module16FinancialHandoffContract
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProfitabilityReconciliationTest {

    private lateinit var reconciliationService: ProfitabilityReconciliationServiceImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"

    @Before
    fun setUp() {
        val fakeHandoffAdapter = object : Module16FinancialHandoffAdapter {
            override suspend fun getVerifiedFinancialHandoff(
                tenantId: String,
                projectId: String,
                periodId: String
            ): DomainResult<ValidatedFinancialHandoff> {
                return DomainResult.Success(
                    ValidatedFinancialHandoff(
                        contract = Module16FinancialHandoffContract(
                            tenantId = tenantId,
                            projectId = projectId,
                            periodId = periodId,
                            periodCode = "2026-M08",
                            isPeriodClosed = false,
                            closureCertificateChecksum = null,
                            isLedgerBalanced = true,
                            totalRecognizedRevenue = BigDecimal("50000.0000"),
                            totalDirectExpenses = BigDecimal("30000.0000")
                        ),
                        integrityStatus = SourceIntegrityStatus.VERIFIED,
                        isLedgerBalanced = true,
                        isPeriodClosed = false,
                        hasValidClosureCertificate = false,
                        validationNotes = emptyList()
                    )
                )
            }

            override suspend fun verifyPeriodIntegrityStatus(
                tenantId: String,
                projectId: String,
                periodId: String
            ): DomainResult<SourceIntegrityStatus> {
                return DomainResult.Success(SourceIntegrityStatus.VERIFIED)
            }
        }

        val sourceRegistry = ProfitabilitySourceRegistryImpl(fakeHandoffAdapter)
        reconciliationService = ProfitabilityReconciliationServiceImpl(fakeHandoffAdapter, sourceRegistry)
    }

    @Test
    fun testPerfectReconciliation() = runBlocking {
        val rev1 = RevenueProvenance("R1", tenantId, projectId, RevenueSourceType.CUSTOMER_INVOICE, "INV-1", recognizedAmount = BigDecimal("50000.0000"))
        val cost1 = CostAttributionReference("C1", tenantId, projectId, CostAttributionSourceType.EXPENSE, "E1", CostComponentType.MATERIAL, attributableAmount = BigDecimal("20000.0000"))
        val cost2 = CostAttributionReference("C2", tenantId, projectId, CostAttributionSourceType.PAYABLE, "P1", CostComponentType.LABOUR, attributableAmount = BigDecimal("10000.0000"))

        val snapshot = ProfitabilitySnapshot(
            id = "SNAP-1",
            tenantId = tenantId,
            projectId = projectId,
            scope = ProfitabilityScope.JOB,
            targetEntityId = "JOB-101",
            periodId = "PER-2026-M08",
            metrics = ProfitabilityMetric(
                revenue = BigDecimal("50000.0000"),
                directCost = BigDecimal("30000.0000"),
                indirectCost = BigDecimal.ZERO,
                totalCost = BigDecimal("30000.0000"),
                grossProfit = BigDecimal("20000.0000"),
                grossMarginPercentage = BigDecimal("40.0000")
            ),
            costBreakdowns = listOf(
                CostComponentBreakdown(CostComponentType.MATERIAL, BigDecimal("20000.0000"), BigDecimal("66.6667"), 1),
                CostComponentBreakdown(CostComponentType.LABOUR, BigDecimal("10000.0000"), BigDecimal("33.3333"), 1)
            ),
            revenueProvenances = listOf(rev1),
            costAttributions = listOf(cost1, cost2),
            generatedBy = "USER-1"
        )

        val res = reconciliationService.reconcileSnapshot(snapshot)
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data

        assertTrue(event.isReconciled)
        assertEquals(0, event.discrepancies.size)
        assertEquals(BigDecimal("0.0000"), event.revenueDifference)
        assertEquals(BigDecimal("0.0000"), event.costDifference)
    }

    @Test
    fun testRevenueDiscrepancyDetection() = runBlocking {
        val rev1 = RevenueProvenance("R1", tenantId, projectId, RevenueSourceType.CUSTOMER_INVOICE, "INV-1", recognizedAmount = BigDecimal("45000.0000")) // Prov says 45k
        val cost1 = CostAttributionReference("C1", tenantId, projectId, CostAttributionSourceType.EXPENSE, "E1", CostComponentType.MATERIAL, attributableAmount = BigDecimal("30000.0000"))

        val snapshot = ProfitabilitySnapshot(
            id = "SNAP-2",
            tenantId = tenantId,
            projectId = projectId,
            scope = ProfitabilityScope.JOB,
            targetEntityId = "JOB-102",
            metrics = ProfitabilityMetric(
                revenue = BigDecimal("50000.0000"), // Snapshot says 50k -> 5k discrepancy!
                directCost = BigDecimal("30000.0000"),
                totalCost = BigDecimal("30000.0000"),
                grossProfit = BigDecimal("20000.0000"),
                grossMarginPercentage = BigDecimal("40.0000")
            ),
            revenueProvenances = listOf(rev1),
            costAttributions = listOf(cost1),
            generatedBy = "USER-1"
        )

        val res = reconciliationService.reconcileSnapshot(snapshot)
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data

        assertFalse(event.isReconciled)
        assertEquals(BigDecimal("5000.0000"), event.revenueDifference)
        assertTrue(event.discrepancies.any { it.contains("differs from provenance sum") })
    }

    @Test
    fun testCostDiscrepancyDetection() = runBlocking {
        val rev1 = RevenueProvenance("R1", tenantId, projectId, RevenueSourceType.CUSTOMER_INVOICE, "INV-1", recognizedAmount = BigDecimal("50000.0000"))
        val cost1 = CostAttributionReference("C1", tenantId, projectId, CostAttributionSourceType.EXPENSE, "E1", CostComponentType.MATERIAL, attributableAmount = BigDecimal("25000.0000")) // Prov says 25k

        val snapshot = ProfitabilitySnapshot(
            id = "SNAP-3",
            tenantId = tenantId,
            projectId = projectId,
            scope = ProfitabilityScope.JOB,
            targetEntityId = "JOB-103",
            metrics = ProfitabilityMetric(
                revenue = BigDecimal("50000.0000"),
                directCost = BigDecimal("30000.0000"), // Snapshot says 30k -> 5k discrepancy!
                totalCost = BigDecimal("30000.0000"),
                grossProfit = BigDecimal("20000.0000"),
                grossMarginPercentage = BigDecimal("40.0000")
            ),
            revenueProvenances = listOf(rev1),
            costAttributions = listOf(cost1),
            generatedBy = "USER-1"
        )

        val res = reconciliationService.reconcileSnapshot(snapshot)
        assertTrue(res is DomainResult.Success)
        val event = (res as DomainResult.Success).data

        assertFalse(event.isReconciled)
        assertEquals(BigDecimal("5000.0000"), event.costDifference)
        assertTrue(event.discrepancies.any { it.contains("differs from attribution sum") })
    }
}

package com.sucharu.sucharupro.ui.features.profitability

import com.sucharu.sucharupro.data.api.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProfitCostAnalysisFoundationUiTest {

    @Test
    fun testProfitabilitySnapshotDtoMapping() {
        val metricDto = ProfitabilityMetricDto(
            revenue = BigDecimal("150000.0000"),
            directCost = BigDecimal("95000.0000"),
            indirectCost = BigDecimal("15000.0000"),
            totalCost = BigDecimal("110000.0000"),
            grossProfit = BigDecimal("40000.0000"),
            grossMarginPercentage = BigDecimal("26.6667"),
            baselineCost = BigDecimal("105000.0000"),
            costVariance = BigDecimal("5000.0000")
        )

        val breakdown = CostComponentBreakdownDto(
            componentType = "MATERIAL",
            totalAmount = BigDecimal("60000.0000"),
            percentageOfTotalCost = BigDecimal("54.5455"),
            itemCount = 4
        )

        val revProv = RevenueProvenanceDto(
            id = "REV-1",
            tenantId = "T1",
            projectId = "P1",
            canonicalSourceType = "CUSTOMER_INVOICE",
            canonicalSourceId = "INV-100",
            recognizedAmount = BigDecimal("150000.0000")
        )

        val snapshotDto = ProfitabilitySnapshotDto(
            id = "SNAP-100",
            tenantId = "T1",
            projectId = "P1",
            scope = "BUSINESS",
            targetEntityId = null,
            periodId = "PER-2026-M08",
            currency = "BDT",
            metrics = metricDto,
            costBreakdowns = listOf(breakdown),
            revenueProvenances = listOf(revProv),
            costAttributions = emptyList(),
            calculationVersion = "1.0.0",
            sourceIntegrityStatus = "VERIFIED",
            financialHandoffVerified = true,
            handoffChecksum = "hash123",
            integrityNotes = listOf("Verified"),
            generatedBy = "USER-1",
            generatedAt = System.currentTimeMillis()
        )

        assertEquals("SNAP-100", snapshotDto.id)
        assertEquals("BUSINESS", snapshotDto.scope)
        assertEquals(BigDecimal("150000.0000"), snapshotDto.metrics.revenue)
        assertEquals(BigDecimal("40000.0000"), snapshotDto.metrics.grossProfit)
        assertEquals("VERIFIED", snapshotDto.sourceIntegrityStatus)
        assertTrue(snapshotDto.financialHandoffVerified)
        assertEquals(1, snapshotDto.costBreakdowns.size)
        assertEquals(1, snapshotDto.revenueProvenances.size)
    }

    @Test
    fun testProfitabilitySourceReadinessDtoStructure() {
        val readiness = ProfitabilitySourceReadinessDto(
            tenantId = "T1",
            projectId = "P1",
            periodId = "PER-2026-M08",
            module15HandoffStatus = "VERIFIED",
            isLedgerBalanced = true,
            directExpensesAvailable = true,
            vendorPayablesAvailable = true,
            recognizedRevenueAvailable = true,
            costAllocationsAvailable = true,
            activeCommitmentsCount = 0,
            outstandingAccrualsCount = 0,
            periodClosed = false,
            warnings = emptyList(),
            evaluatedAt = System.currentTimeMillis()
        )

        assertEquals("VERIFIED", readiness.module15HandoffStatus)
        assertTrue(readiness.isLedgerBalanced)
        assertTrue(readiness.directExpensesAvailable)
    }

    @Test
    fun testProfitabilityReconciliationEventDtoStructure() {
        val event = ProfitabilityReconciliationEventDto(
            id = "REC-1",
            tenantId = "T1",
            projectId = "P1",
            snapshotId = "SNAP-1",
            scope = "JOB",
            targetEntityId = "JOB-101",
            periodId = "PER-2026-M08",
            isReconciled = true,
            canonicalRevenueTotal = BigDecimal("50000.0000"),
            snapshotRevenueTotal = BigDecimal("50000.0000"),
            revenueDifference = BigDecimal("0.0000"),
            canonicalCostTotal = BigDecimal("30000.0000"),
            snapshotCostTotal = BigDecimal("30000.0000"),
            costDifference = BigDecimal("0.0000"),
            discrepancies = emptyList(),
            checkedBy = "USER-1",
            checkedAt = System.currentTimeMillis()
        )

        assertTrue(event.isReconciled)
        assertEquals(BigDecimal("0.0000"), event.revenueDifference)
        assertEquals(BigDecimal("0.0000"), event.costDifference)
    }
}

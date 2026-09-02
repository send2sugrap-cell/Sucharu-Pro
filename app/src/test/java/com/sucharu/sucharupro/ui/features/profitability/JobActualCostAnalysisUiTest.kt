package com.sucharu.sucharupro.ui.features.profitability

import com.sucharu.sucharupro.data.api.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class JobActualCostAnalysisUiTest {

    @Test
    fun testJobCostSnapshotDtoMapping() {
        val compDto = JobCostComponentDto(
            componentId = "COMP-01",
            componentType = "MATERIAL_COST",
            directness = "DIRECT",
            quantity = BigDecimal("500.0000"),
            unitRate = BigDecimal("10.0000"),
            originalAmount = BigDecimal("5000.0000"),
            attributedAmount = BigDecimal("5000.0000"),
            percentageOfTotalCost = BigDecimal("62.5000"),
            currency = "BDT",
            attributionBasis = "INVENTORY_STOCKOUT",
            sourceItemCount = 1,
            calculationExplanation = "Paper stock consumption"
        )

        val provDto = JobCostProvenanceDto(
            provenanceId = "PROV-01",
            sourceModule = "MODULE_08",
            sourceEntityType = "STOCK_OUT",
            sourceEntityId = "SO-101",
            sourceTransactionId = "TX-1",
            sourceReference = "REF-101",
            costComponentType = "MATERIAL_COST",
            directness = "DIRECT",
            originalAmount = BigDecimal("5000.0000"),
            attributedAmount = BigDecimal("5000.0000"),
            currency = "BDT",
            attributionBasis = "STOCK_OUT_RECORD",
            calculationExplanation = "Paper consumption",
            fingerprintHash = "MODULE_08:STOCK_OUT:SO-101:TX-1:MATERIAL_COST"
        )

        val snapshotDto = JobCostSnapshotDto(
            snapshotId = "JOB-SNAP-01",
            tenantId = "TENANT-1",
            projectId = "PROJ-1",
            jobId = "JOB-100",
            jobNumber = "JOB-2026-100",
            customerId = "CUST-1",
            productId = "PROD-1",
            jobQuantity = 1000,
            calculationVersion = "JOB_COST_ENGINE_V1",
            calculationTimestamp = System.currentTimeMillis(),
            currency = "BDT",
            totalActualCost = BigDecimal("8000.0000"),
            totalDirectCost = BigDecimal("6000.0000"),
            totalIndirectCost = BigDecimal("2000.0000"),
            estimatedCost = BigDecimal("7500.0000"),
            costVariance = BigDecimal("500.0000"),
            costVariancePercentage = BigDecimal("6.6667"),
            varianceClassification = "OVER_BUDGET",
            readinessStatus = "COMPLETE",
            isReconciled = true,
            sourceCount = 1,
            duplicateSourceCount = 0,
            unresolvedSourceCount = 0,
            costComponents = listOf(compDto),
            provenances = listOf(provDto),
            warnings = emptyList(),
            integrityHash = "hash1234567890",
            generatedBy = "ADMIN-1"
        )

        assertEquals("JOB-SNAP-01", snapshotDto.snapshotId)
        assertEquals("JOB-100", snapshotDto.jobId)
        assertEquals(BigDecimal("8000.0000"), snapshotDto.totalActualCost)
        assertEquals(BigDecimal("6000.0000"), snapshotDto.totalDirectCost)
        assertEquals(BigDecimal("2000.0000"), snapshotDto.totalIndirectCost)
        assertEquals(BigDecimal("500.0000"), snapshotDto.costVariance)
        assertEquals("OVER_BUDGET", snapshotDto.varianceClassification)
        assertEquals("COMPLETE", snapshotDto.readinessStatus)
        assertEquals(1, snapshotDto.costComponents.size)
        assertEquals("MATERIAL_COST", snapshotDto.costComponents[0].componentType)
        assertEquals(1, snapshotDto.provenances.size)
    }

    @Test
    fun testJobCostVarianceDtoStructure() {
        val varianceDto = JobCostVarianceDto(
            jobId = "JOB-100",
            actualCost = BigDecimal("9500.0000"),
            estimatedCost = BigDecimal("10000.0000"),
            costVariance = BigDecimal("-500.0000"),
            costVariancePercentage = BigDecimal("-5.0000"),
            classification = "UNDER_BUDGET",
            explanation = "5% under budget"
        )

        assertEquals("JOB-100", varianceDto.jobId)
        assertEquals(BigDecimal("-500.0000"), varianceDto.costVariance)
        assertEquals("UNDER_BUDGET", varianceDto.classification)
    }
}

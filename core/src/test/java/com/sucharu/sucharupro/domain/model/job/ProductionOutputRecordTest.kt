package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionOutputRecordTest {

    @Test
    fun createValidOutputRecord_setsAllProperties() {
        val output = ProductionStageOutput(
            outputId = "out-01",
            jobId = "job-01",
            stageId = "stg-01",
            stageType = ProductionStageType.PRINTING,
            quantity = 500,
            unit = "কপি",
            recordedAt = "2026-08-16T10:00:00Z",
            operatorId = "op-01",
            operatorName = "Rahim Ahmed",
            executionId = "exec-01",
            remarks = "৫০০ কপি মুদ্রণ সম্পন্ন"
        )

        assertEquals("out-01", output.outputId)
        assertEquals("job-01", output.jobId)
        assertEquals("stg-01", output.stageId)
        assertEquals(ProductionStageType.PRINTING, output.stageType)
        assertEquals(500, output.quantity)
        assertEquals("কপি", output.unit)
        assertEquals("op-01", output.operatorId)
        assertEquals("Rahim Ahmed", output.operatorName)
        assertEquals("exec-01", output.executionId)
        assertEquals("৫০০ কপি মুদ্রণ সম্পন্ন", output.remarks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroQuantity_throwsException() {
        ProductionStageOutput(
            outputId = "out-01",
            jobId = "job-01",
            stageId = "stg-01",
            stageType = ProductionStageType.PRINTING,
            quantity = 0,
            unit = "কপি",
            recordedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeQuantity_throwsException() {
        ProductionStageOutput(
            outputId = "out-01",
            jobId = "job-01",
            stageId = "stg-01",
            stageType = ProductionStageType.PRINTING,
            quantity = -50,
            unit = "কপি",
            recordedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankUnit_throwsException() {
        ProductionStageOutput(
            outputId = "out-01",
            jobId = "job-01",
            stageId = "stg-01",
            stageType = ProductionStageType.PRINTING,
            quantity = 100,
            unit = "  ",
            recordedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test
    fun reconciliationModel_calculatesDerivedFlags() {
        val reconciliation = ProductionOutputReconciliation(
            jobId = "job-01",
            jobNumber = "JOB-2026-0001",
            plannedQuantity = 1000,
            recordedQuantity = 1050,
            remainingQuantity = 0,
            overProductionQuantity = 50,
            underProductionQuantity = 0,
            completionPercentage = 105.0,
            unit = "কপি",
            outputRecordCount = 3
        )

        assertTrue(reconciliation.isFullyProduced)
        assertTrue(reconciliation.isOverProduced)
        assertEquals("105.0%", reconciliation.formattedCompletionPercentage)
    }
}

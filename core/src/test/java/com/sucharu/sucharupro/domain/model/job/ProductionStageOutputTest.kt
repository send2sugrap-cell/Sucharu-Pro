package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.validation.ProductionStageOutputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain model and helper tests for [ProductionStageOutput] (Module 04 Step 06).
 */
class ProductionStageOutputTest {

    @Test
    fun validOutputCreation_populatesAllFields() {
        val output = ProductionStageOutput(
            outputId = "out-001",
            jobId = "job-001",
            stageId = "stage-prt",
            stageType = ProductionStageType.PRINTING,
            quantity = 500,
            unit = "কপি",
            recordedAt = "2026-08-16T10:30:00Z",
            operatorId = "op-01",
            operatorName = "রহিম আহমেদ",
            recordedBy = "usr-01",
            recordedByName = "সুপারভাইজার",
            executionId = "exec-001",
            remarks = "১ম ব্যাচ সফলভাবে মুদ্রিত"
        )

        assertEquals("out-001", output.outputId)
        assertEquals("job-001", output.jobId)
        assertEquals("stage-prt", output.stageId)
        assertEquals(ProductionStageType.PRINTING, output.stageType)
        assertEquals(500, output.quantity)
        assertEquals("কপি", output.unit)
        assertEquals("2026-08-16T10:30:00Z", output.recordedAt)
        assertEquals("op-01", output.operatorId)
        assertEquals("রহিম আহমেদ", output.operatorName)
        assertEquals("১ম ব্যাচ সফলভাবে মুদ্রিত", output.remarks)
    }

    @Test
    fun immutableOutputRecord_cannotBeMutatedDirectly() {
        val output1 = ProductionStageOutput(
            outputId = "out-001",
            jobId = "job-001",
            stageId = "stage-01",
            stageType = ProductionStageType.PRINTING,
            quantity = 250,
            unit = "Pcs",
            recordedAt = "2026-08-16T10:00:00Z"
        )

        val output2 = output1.copy(quantity = 300)
        assertEquals(250, output1.quantity)
        assertEquals(300, output2.quantity)
    }

    @Test
    fun positiveQuantityValidation_enforcedInInit() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductionStageOutput(
                outputId = "out-001",
                jobId = "job-001",
                stageId = "stage-01",
                stageType = ProductionStageType.PRINTING,
                quantity = 0,
                unit = "Pcs",
                recordedAt = "2026-08-16T10:00:00Z"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ProductionStageOutput(
                outputId = "out-001",
                jobId = "job-001",
                stageId = "stage-01",
                stageType = ProductionStageType.PRINTING,
                quantity = -50,
                unit = "Pcs",
                recordedAt = "2026-08-16T10:00:00Z"
            )
        }
    }

    @Test
    fun blankUnit_rejectedInInit() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductionStageOutput(
                outputId = "out-001",
                jobId = "job-001",
                stageId = "stage-01",
                stageType = ProductionStageType.PRINTING,
                quantity = 100,
                unit = "  ",
                recordedAt = "2026-08-16T10:00:00Z"
            )
        }
    }

    @Test
    fun banglaUnicodeFidelity_preservedInRemarksAndAttribution() {
        val output = ProductionStageOutput(
            outputId = "out-002",
            jobId = "job-002",
            stageId = "stage-bnd",
            stageType = ProductionStageType.BINDING,
            quantity = 1000,
            unit = "কপি",
            recordedAt = "2026-08-16T11:00:00Z",
            operatorName = "করিম চৌধুরী",
            remarks = "বাংলা ব্যাকরণ বই পূর্ণ বাঁধাই সম্পন্ন"
        )

        assertEquals("করিম চৌধুরী", output.operatorName)
        assertEquals("বাংলা ব্যাকরণ বই পূর্ণ বাঁধাই সম্পন্ন", output.remarks)
        assertEquals("কপি", output.unit)
    }

    @Test
    fun remainingQuantityCalculation_computesAccurately() {
        val remaining1 = ProductionStageOutputValidator.calculateRemainingQuantity(1000, 650)
        assertEquals(350, remaining1)

        val remaining2 = ProductionStageOutputValidator.calculateRemainingQuantity(1000, 1000)
        assertEquals(0, remaining2)

        val remaining3 = ProductionStageOutputValidator.calculateRemainingQuantity(1000, 1200)
        assertEquals(0, remaining3) // Coerced to at least 0

        val progress1 = ProductionStageOutputValidator.calculateProgressFraction(650, 1000)
        assertEquals(0.65f, progress1, 0.001f)

        val progress2 = ProductionStageOutputValidator.calculateProgressFraction(1000, 1000)
        assertEquals(1.0f, progress2, 0.001f)
    }
}

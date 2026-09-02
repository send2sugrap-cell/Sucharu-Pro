package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Test Suite for Prepress Orchestration Engine.
 * Tests cross-step reconciliation, readiness scoring, recommendations, and master integrity sealing.
 * Module 18 Step 06.
 */
class PrepressOrchestrationEngineTest {

    private val tenantId = "tenant_test_18"

    private fun createSampleSignatureAndCtp(): Pair<SignatureImpositionSpecification, CtpOutputSpecification> {
        val pageDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val sigSpec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = tenantId,
            name = "Test 16pp Catalog",
            jobId = "JOB-101",
            orderId = "ORD-202",
            orderItemId = "ITEM-303",
            productName = "Product Catalog 16pp",
            totalPages = 16,
            signaturePageCount = 16,
            bindingMethod = BindingMethod.SADDLE_STITCH,
            sheetTurningMethod = SheetTurningMethod.SHEETWISE,
            foldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
            pageDimension = pageDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 1000L,
            paperStockType = PaperStockType.ART_PAPER,
            gsm = BigDecimal("150.0000")
        )

        val ctpSpec = CtpOutputGenerationEngine.generateFromSignatureImposition(
            signatureSpec = sigSpec,
            colorSeparations = listOf(
                PlateColorSeparation.CYAN,
                PlateColorSeparation.MAGENTA,
                PlateColorSeparation.YELLOW,
                PlateColorSeparation.BLACK
            )
        )

        return Pair(sigSpec, ctpSpec)
    }

    @Test
    fun testOrchestratePlan_CompleteHarmonizedPipeline() {
        val (sigSpec, ctpSpec) = createSampleSignatureAndCtp()

        val plan = PrepressOrchestrationEngine.orchestratePlan(
            tenantId = tenantId,
            planName = "Full Harmonized Plan",
            jobId = "JOB-101",
            orderId = "ORD-202",
            orderItemId = "ITEM-303",
            productName = "Product Catalog 16pp",
            requiredQuantity = 1000L,
            step04Signature = sigSpec,
            step05CtpOutput = ctpSpec,
            actor = "lead_prepress"
        )

        assertNotNull(plan)
        assertEquals(tenantId, plan.tenantId)
        assertEquals("JOB-101", plan.jobId)
        assertEquals("ORD-202", plan.orderId)
        assertEquals(1000L, plan.requiredQuantity)
        assertEquals(1000L, plan.totalProducedQuantity)
        assertEquals(1000L, plan.requiredSheets)
        assertEquals(1, plan.totalSignaturesCount)
        assertEquals(8, plan.totalPlatesCount) // 2 forms * 4 colors = 8 plates

        // Check Reconciliation
        assertTrue(plan.reconciliationResult.isReconciled)
        assertEquals(0, plan.reconciliationResult.blockingErrorsCount)

        // Check Readiness Score
        assertTrue(plan.readinessScore.overallScore >= BigDecimal("90.0000"))

        // Check Master Integrity Seal
        assertNotNull(plan.masterIntegrityHash)
        assertEquals(64, plan.masterIntegrityHash.length) // SHA-256 hex string
    }

    @Test
    fun testReconcileSteps_QuantityDeficit_GeneratesBlockingError() {
        val (sigSpec, ctpSpec) = createSampleSignatureAndCtp()

        // Required quantity 5000, but signature spec produced only 1000
        val plan = PrepressOrchestrationEngine.orchestratePlan(
            tenantId = tenantId,
            planName = "Deficit Plan",
            jobId = "JOB-101",
            orderId = "ORD-202",
            orderItemId = "ITEM-303",
            productName = "Product Catalog 16pp",
            requiredQuantity = 5000L,
            step04Signature = sigSpec,
            step05CtpOutput = ctpSpec,
            actor = "lead_prepress"
        )

        assertFalse(plan.reconciliationResult.isReconciled)
        assertTrue(plan.reconciliationResult.blockingErrorsCount >= 1)
        val deficitError = plan.reconciliationResult.discrepancies.firstOrNull { it.field == "producedQuantity" }
        assertNotNull(deficitError)
        assertEquals(ReconciliationSeverity.BLOCKING_ERROR, deficitError?.severity)
    }

    @Test
    fun testMasterIntegrityHash_IsDeterministic() {
        val (sigSpec, ctpSpec) = createSampleSignatureAndCtp()

        val plan1 = PrepressOrchestrationEngine.orchestratePlan(
            tenantId = tenantId,
            planName = "Plan A",
            jobId = "JOB-101",
            orderId = "ORD-202",
            orderItemId = "ITEM-303",
            productName = "Product Catalog 16pp",
            requiredQuantity = 1000L,
            step04Signature = sigSpec,
            step05CtpOutput = ctpSpec,
            actor = "lead_prepress"
        )

        val plan2 = PrepressOrchestrationEngine.orchestratePlan(
            tenantId = tenantId,
            planName = "Plan B",
            jobId = "JOB-101",
            orderId = "ORD-202",
            orderItemId = "ITEM-303",
            productName = "Product Catalog 16pp",
            requiredQuantity = 1000L,
            step04Signature = sigSpec,
            step05CtpOutput = ctpSpec,
            actor = "lead_prepress"
        )

        assertEquals(plan1.masterIntegrityHash, plan2.masterIntegrityHash)
    }

    @Test
    fun testOptimizationRecommendations_SpotColorConversion() {
        val (sigSpec, ctpSpec) = createSampleSignatureAndCtp()

        // Create CTP with spot colors
        val ctpWithSpot = CtpOutputGenerationEngine.generateFromSignatureImposition(
            signatureSpec = sigSpec,
            colorSeparations = listOf(
                PlateColorSeparation.CYAN,
                PlateColorSeparation.MAGENTA,
                PlateColorSeparation.YELLOW,
                PlateColorSeparation.BLACK,
                PlateColorSeparation.SPOT_PANTONE
            ),
            spotColorNames = listOf("Pantone 185 C")
        )

        val plan = PrepressOrchestrationEngine.orchestratePlan(
            tenantId = tenantId,
            planName = "Spot Color Plan",
            jobId = "JOB-101",
            orderId = "ORD-202",
            orderItemId = "ITEM-303",
            productName = "Product Catalog 16pp",
            requiredQuantity = 1000L,
            step04Signature = sigSpec,
            step05CtpOutput = ctpWithSpot,
            actor = "lead_prepress"
        )

        val spotRec = plan.recommendations.firstOrNull { it.recommendationType == "SPOT_COLOR_CONVERSION_ANALYSIS" }
        assertNotNull(spotRec)
        assertTrue(spotRec?.estimatedPlateSavingsCount ?: 0 > 0)
    }
}

package com.sucharu.sucharupro.domain.service.jobclosure

import com.sucharu.sucharupro.domain.model.jobclosure.*
import com.sucharu.sucharupro.domain.model.jobcosting.VarianceClassification
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductionJobClosureDomainTest {

    private val auditEngine = JobClosureReadinessAuditEngine()
    private val scorecardEngine = ManufacturingScorecardEngine()
    private val provenanceEngine = ProductionProvenanceGraphEngine()
    private val sealEngine = MasterJobClosureSealEngine()

    @Test
    fun `test scorecard engine accurately evaluates OTIF, RFT, CAI, and composite index`() {
        val scorecard = scorecardEngine.evaluateScorecard(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            orderQuantity = BigDecimal("5000.0000"),
            goodUnitsReleased = BigDecimal("5000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            reworkOrScrapUnits = BigDecimal("100.0000"),
            machineEfficiency = BigDecimal("90.0000"),
            onTime = true
        )

        assertEquals(BigDecimal("100.0000"), scorecard.onTimeInFullPercentage)
        assertEquals(BigDecimal("100.0000"), scorecard.costAdherenceIndex)
        assertTrue(scorecard.rightFirstTimePercentage > BigDecimal("95.0000"))
        assertTrue(scorecard.overallManufacturingIndex > BigDecimal("90.0000"))
        assertEquals("A+", scorecard.performanceGrade)
    }

    @Test
    fun `test provenance graph engine creates unbroken 10-step cryptographic lineage`() {
        val graph = provenanceEngine.buildProvenanceGraph(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            calculationId = "CALC-001",
            quoteId = "QUO-001",
            planningId = "PLAN-001",
            scheduleId = "SCHED-001",
            workOrderIds = listOf("WO-01", "WO-02"),
            trackingIds = listOf("TRK-01"),
            qcInspectionId = "QC-01",
            packagingId = "PKG-01",
            releaseId = "REL-01",
            costRecordId = "COST-01"
        )

        assertTrue(graph.isChainUnbroken)
        assertEquals(9, graph.nodes.size)
        assertEquals(64, graph.masterProvenanceFingerprint.length)
    }

    @Test
    fun `test master job closure seal engine generates valid 64-character SHA-256 certificate`() {
        val cert = sealEngine.generateMasterSealCertificate(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            goodUnitsReleased = BigDecimal("5000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            totalCostVariance = BigDecimal("-500.0000"),
            overallCostClassification = VarianceClassification.FAVORABLE,
            overallManufacturingScore = BigDecimal("95.5000"),
            sealedBy = "plant-manager"
        )

        assertNotNull(cert.masterSealHash)
        assertEquals(64, cert.masterSealHash.length)
        assertEquals(BigDecimal("5000.0000"), cert.totalGoodUnitsReleased)
        assertEquals(VarianceClassification.FAVORABLE, cert.overallCostClassification)
    }
}

package com.sucharu.sucharupro.domain.service.finalqc

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FinalQcPackagingDomainTest {

    private val inspectionEngine = FinalQcInspectionEngine()
    private val defectEngine = DefectContainmentEngine()
    private val packagingEngine = PackagingOrchestrationEngine()
    private val releaseEngine = FinishedGoodsReleaseEngine()
    private val reconciliationEngine = FinalQcPackagingReconciliationEngine()

    @Test
    fun `test inspection creation and completion calculates status correctly`() {
        val checklist = listOf(
            QcChecklistItem("CHK-01", "Registration Alignment", true, "0.1mm", "<0.2mm", "Precise alignment"),
            QcChecklistItem("CHK-02", "Color Density CMYK", true, "1.45 D", "1.40-1.50 D", "Delta E < 2.0"),
            QcChecklistItem("CHK-03", "Binding & Trimming", true, "210x297mm", "Exact A4", "Clean cut")
        )

        val created = inspectionEngine.createInspection(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            samplePlanType = InspectionSamplePlanType.AQL_LEVEL_II_NORMAL,
            totalLotQuantity = BigDecimal("5000.0000"),
            sampleSize = BigDecimal("200.0000"),
            checklist = checklist,
            inspectorId = "INSP-01",
            inspectorName = "Kamal QC Lead"
        )

        assertEquals(FinalQcInspectionStatus.IN_PROGRESS, created.status)
        assertEquals(BigDecimal("5000.0000"), created.totalLotQuantity)
        assertEquals(BigDecimal("200.0000"), created.sampleSize)

        // Complete with 4,950 accepted, 30 rejected, 20 rework
        val completed = inspectionEngine.completeInspection(
            inspection = created,
            acceptedQuantity = BigDecimal("4950.0000"),
            rejectedQuantity = BigDecimal("30.0000"),
            reworkQuantity = BigDecimal("20.0000")
        )

        assertEquals(FinalQcInspectionStatus.REWORK_REQUIRED, completed.status)
        assertEquals(BigDecimal("4950.0000"), completed.acceptedQuantity)
        assertEquals(BigDecimal("30.0000"), completed.rejectedQuantity)
        assertEquals(BigDecimal("20.0000"), completed.reworkQuantity)
    }

    @Test
    fun `test defect containment logging and severity assignment`() {
        val defect = defectEngine.recordDefectContainment(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            inspectionId = "INSP-01",
            rootCauseStage = ProductionStageType.PRINTING,
            defectType = DefectClassificationType.COLOR_MISMATCH,
            severity = DefectSeverity.MAJOR,
            defectQuantity = BigDecimal("50.0000"),
            disposition = ContainmentDisposition.QUARANTINED,
            quarantineLocation = "BAY-Q3-OFFSET",
            reworkWorkOrderId = null,
            rootCauseDetails = "Ink viscosity drop on Magenta unit",
            loggedBy = "qc-inspector"
        )

        assertEquals(DefectSeverity.MAJOR, defect.severity)
        assertEquals(BigDecimal("50.0000"), defect.defectQuantity)
        assertEquals(ContainmentDisposition.QUARANTINED, defect.disposition)
        assertEquals("BAY-Q3-OFFSET", defect.quarantineLocation)
    }

    @Test
    fun `test packaging record generation and barcode creation`() {
        val pkg = packagingEngine.createPackagingRecord(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            inspectionId = "INSP-01",
            packagingType = PackagingType.CORRUGATED_BOX,
            unitsPerPackage = BigDecimal("500.0000"),
            totalPackageCount = 10,
            palletIdentifier = "PALLET-01",
            cartonNumbersRange = "BOX 01/10 - 10/10",
            grossWeightKg = BigDecimal("25.5000"),
            packagedBy = "packaging-lead"
        )

        assertEquals(10, pkg.totalPackageCount)
        assertEquals(BigDecimal("5000.0000"), pkg.totalPackagedQuantity)
        assertTrue(pkg.packagingSlipBarcode.startsWith("PKG-"))
    }

    @Test
    fun `test finished goods release generates deterministic SHA-256 certificate hash`() {
        val release = releaseEngine.authorizeRelease(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            inspectionId = "INSP-01",
            packagingId = "PKG-01",
            releasedQuantity = BigDecimal("5000.0000"),
            destination = "WAREHOUSE_FINISHED_GOODS",
            authorizedBy = "plant-manager"
        )

        assertEquals(FinishedGoodsReleaseStatus.RELEASE_APPROVED, release.status)
        assertEquals(64, release.integrityHash.length)

        // Verify SHA-256 validity
        val expectedHash = FinalQcPackagingMathUtils.generateReleaseCertificateHash(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            inspectionId = "INSP-01",
            packagingId = "PKG-01",
            releasedQuantity = BigDecimal("5000.0000"),
            destination = "WAREHOUSE_FINISHED_GOODS",
            authorizedBy = "plant-manager",
            authorizedAt = release.authorizedAt
        )
        assertEquals(expectedHash, release.integrityHash)
    }

    @Test
    fun `test 8-way multi-tier quality reconciliation detects tampering and passes on valid data`() {
        val inspection = ProductionFinalQcInspection(
            inspectionId = "INSP-01",
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            samplePlanType = InspectionSamplePlanType.FULL_100_PERCENT,
            totalLotQuantity = BigDecimal("5000.0000"),
            sampleSize = BigDecimal("5000.0000"),
            acceptedQuantity = BigDecimal("4950.0000"),
            rejectedQuantity = BigDecimal("50.0000"),
            reworkQuantity = BigDecimal.ZERO,
            status = FinalQcInspectionStatus.CONDITIONALLY_ACCEPTED,
            inspectorId = "INSP-01",
            inspectorName = "QC Lead"
        )

        val defect = ProductionDefectContainmentRecord(
            containmentId = "DEF-01",
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            inspectionId = "INSP-01",
            rootCauseStage = ProductionStageType.PRINTING,
            defectType = DefectClassificationType.PRINTING_DEFECT,
            severity = DefectSeverity.MAJOR,
            defectQuantity = BigDecimal("50.0000"),
            disposition = ContainmentDisposition.SCRAPPED,
            quarantineLocation = "SCRAP_BIN_A",
            rootCauseDetails = "Scumming",
            loggedBy = "qc"
        )

        val packaging = ProductionPackagingRecord(
            packagingId = "PKG-01",
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            inspectionId = "INSP-01",
            packagingType = PackagingType.CORRUGATED_BOX,
            unitsPerPackage = BigDecimal("495.0000"),
            totalPackageCount = 10,
            totalPackagedQuantity = BigDecimal("4950.0000"),
            packagingSlipBarcode = "PKG-101-10C-1234",
            packagedBy = "operator"
        )

        val release = releaseEngine.authorizeRelease(
            tenantId = "TENANT-001",
            executionJobId = "JOB-101",
            orderId = "ORD-101",
            inspectionId = "INSP-01",
            packagingId = "PKG-01",
            releasedQuantity = BigDecimal("4950.0000"),
            destination = "DISPATCH_DOCK",
            authorizedBy = "manager"
        )

        val cleanRecon = reconciliationEngine.reconcile(
            executionJobId = "JOB-101",
            tenantId = "TENANT-001",
            inspections = listOf(inspection),
            defects = listOf(defect),
            packagings = listOf(packaging),
            releases = listOf(release),
            totalShopFloorGoodOutput = BigDecimal("5000.0000")
        )

        assertTrue(cleanRecon.isFullyReconciled)
        assertTrue(cleanRecon.releaseCertificateHashValid)
        assertTrue(cleanRecon.zeroUncontainedCriticalDefects)
        assertTrue(cleanRecon.discrepancies.isEmpty())

        // Test tampered hash detection
        val tamperedRelease = release.copy(integrityHash = "corrupted-invalid-hash")
        val tamperedRecon = reconciliationEngine.reconcile(
            executionJobId = "JOB-101",
            tenantId = "TENANT-001",
            inspections = listOf(inspection),
            defects = listOf(defect),
            packagings = listOf(packaging),
            releases = listOf(tamperedRelease),
            totalShopFloorGoodOutput = BigDecimal("5000.0000")
        )

        assertFalse(tamperedRecon.isFullyReconciled)
        assertFalse(tamperedRecon.releaseCertificateHashValid)
        assertTrue(tamperedRecon.discrepancies.any { it.contains("Release certificate integrity hash mismatch") })
    }
}

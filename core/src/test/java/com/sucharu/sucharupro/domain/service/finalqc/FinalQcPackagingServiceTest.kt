package com.sucharu.sucharupro.domain.service.finalqc

import com.sucharu.sucharupro.data.datasource.finalqc.FakeFinalQcPackagingDataSource
import com.sucharu.sucharupro.data.repository.finalqc.FinalQcPackagingRepositoryImpl
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinalQcPackagingServiceTest {

    private lateinit var service: FinalQcPackagingService
    private val tenantId = "TENANT-001"
    private val executionJobId = "JOB-101"
    private val orderId = "ORD-101"

    @Before
    fun setup() {
        val dataSource = FakeFinalQcPackagingDataSource()
        val repository = FinalQcPackagingRepositoryImpl(dataSource)
        service = FinalQcPackagingServiceImpl(repository)
    }

    @Test
    fun `test complete final QC, defect containment, packaging, and release workflow`() = runBlocking {
        // 1. Create inspection
        val checklist = listOf(
            QcChecklistItem("CHK-01", "Registration", true),
            QcChecklistItem("CHK-02", "Color Matching", true)
        )
        val inspection = service.createFinalQcInspection(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            samplePlanType = InspectionSamplePlanType.AQL_LEVEL_II_NORMAL,
            totalLotQuantity = BigDecimal("10000.0000"),
            sampleSize = BigDecimal("315.0000"),
            checklist = checklist,
            inspectorId = "INSP-01",
            inspectorName = "Tariq QC",
            notes = "Standard packaging job"
        )
        assertEquals(FinalQcInspectionStatus.IN_PROGRESS, inspection.status)

        // 2. Complete inspection
        val completedInsp = service.completeFinalQcInspection(
            tenantId = tenantId,
            inspectionId = inspection.inspectionId,
            acceptedQuantity = BigDecimal("9950.0000"),
            rejectedQuantity = BigDecimal("50.0000"),
            reworkQuantity = BigDecimal.ZERO,
            notes = "9950 passed, 50 rejected for registration drift"
        )
        assertEquals(FinalQcInspectionStatus.CONDITIONALLY_ACCEPTED, completedInsp.status)

        // 3. Record defect containment
        val defect = service.recordDefectContainment(
            tenantId = tenantId,
            executionJobId = executionJobId,
            inspectionId = inspection.inspectionId,
            rootCauseStage = ProductionStageType.PRINTING,
            defectType = DefectClassificationType.REGISTRATION_ERROR,
            severity = DefectSeverity.MAJOR,
            defectQuantity = BigDecimal("50.0000"),
            disposition = ContainmentDisposition.SCRAPPED,
            quarantineLocation = "QUARANTINE_BIN_1",
            rootCauseDetails = "Gripper bar adjustment needed",
            actor = "Tariq QC"
        )
        assertEquals(BigDecimal("50.0000"), defect.defectQuantity)

        // 4. Create packaging record
        val pkg = service.createPackagingRecord(
            tenantId = tenantId,
            executionJobId = executionJobId,
            inspectionId = inspection.inspectionId,
            packagingType = PackagingType.CORRUGATED_BOX,
            unitsPerPackage = BigDecimal("500.0000"),
            totalPackageCount = 19,
            palletIdentifier = "PALLET-01",
            cartonNumbersRange = "BOX 01/19 to 19/19",
            grossWeightKg = BigDecimal("45.0000"),
            packagedBy = "Packer 01",
            notes = "Palletized and wrapped"
        )
        assertEquals(BigDecimal("9500.0000"), pkg.totalPackagedQuantity)

        // 5. Authorize finished goods release
        val release = service.authorizeFinishedGoodsRelease(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            inspectionId = inspection.inspectionId,
            packagingId = pkg.packagingId,
            releasedQuantity = BigDecimal("9500.0000"),
            destination = "WAREHOUSE_FINISHED_GOODS",
            authorizedBy = "plant-manager",
            notes = "First dispatch release"
        )
        assertEquals(FinishedGoodsReleaseStatus.RELEASE_APPROVED, release.status)
        assertNotNull(release.integrityHash)

        // 6. Quality variance summary
        val variance = service.getQualityVarianceSummary(tenantId, executionJobId, BigDecimal("10000.0000"))
        assertEquals(BigDecimal("99.5000"), variance.overallQualityYieldPercentage)
        assertEquals(BigDecimal("0.5000"), variance.defectRatePercentage)

        // 7. 8-Way reconciliation
        val recon = service.reconcileFinalQcPackaging(tenantId, executionJobId, BigDecimal("10000.0000"))
        assertTrue(recon.isFullyReconciled)
        assertTrue(recon.releaseCertificateHashValid)

        // 8. AI Handoff contract export
        val handoff = service.getAiHandoffContract(tenantId, executionJobId, orderId)
        assertEquals("1.0.0", handoff.contractVersion)
        assertEquals(BigDecimal("9950.0000"), handoff.totalGoodQuantityAccepted)
        assertEquals(19, handoff.totalPackagedCartons)
        assertTrue(handoff.isFullyReconciled)
    }
}
